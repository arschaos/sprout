package io.arschaos.scanner;

import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import io.arschaos.model.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class JavaParserAnalyzer {

    static {
        ParserConfiguration configuration = new ParserConfiguration();
        configuration.setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_17);
        StaticJavaParser.setConfiguration(configuration);
    }

    private static class ParseContext {
        File file;
        Path relativePath;
        CompilationUnit cu;
        Map<String, String> importMap = new HashMap<>();
        String packageName = "";
        List<ClassDeclarationData> classes = new ArrayList<>();
    }

    private static class ClassDeclarationData {
        ComponentNode node;
        TypeDeclaration<?> typeDeclaration;
        Set<String> fieldNames = new HashSet<>();
        Map<String, String> fieldTypes = new HashMap<>();
        List<String> rawExtends = new ArrayList<>();
        List<String> rawImplements = new ArrayList<>();
        List<MethodCallInfo> methodCalls = new ArrayList<>();
        boolean hasRequiredArgsConstructor;
        boolean hasAllArgsConstructor;
        boolean hasSingleConstructorWithArgs;
    }

    private static class MethodCallInfo {
        String scope;
        String methodName;
    }

    public ArchitectureGraph analyze(File projectDir, List<File> files) {
        ArchitectureGraph graph = new ArchitectureGraph();
        String projectName = projectDir != null ? projectDir.getName() : "project";
        graph.setProjectName(projectName);
        graph.setBaseDirectory(projectDir != null ? projectDir.getAbsolutePath() : "");
        graph.setTimestamp(DateTimeFormatter.ISO_INSTANT.format(Instant.now().atOffset(ZoneOffset.UTC)));

        List<ParseContext> contexts = new ArrayList<>();
        Map<String, ComponentNode> nodeByFqcn = new HashMap<>();
        Map<String, List<ComponentNode>> nodesBySimpleName = new HashMap<>();

        Path basePath = projectDir != null ? projectDir.toPath() : Path.of(".");

        // Pass 1: Parse and build nodes
        for (File file : files) {
            try {
                CompilationUnit cu = StaticJavaParser.parse(file);
                ParseContext ctx = new ParseContext();
                ctx.file = file;
                try {
                    ctx.relativePath = basePath.relativize(file.toPath());
                } catch (Exception e) {
                    ctx.relativePath = file.toPath();
                }
                ctx.cu = cu;

                if (cu.getPackageDeclaration().isPresent()) {
                    ctx.packageName = cu.getPackageDeclaration().get().getNameAsString();
                }

                cu.getImports().forEach(imp -> {
                    String importStr = imp.getNameAsString();
                    String simple = importStr.contains(".") ? importStr.substring(importStr.lastIndexOf('.') + 1) : importStr;
                    ctx.importMap.put(simple, importStr);
                });

                for (TypeDeclaration<?> td : cu.getTypes()) {
                    ClassDeclarationData cdd = extractClassData(td, ctx);
                    ctx.classes.add(cdd);
                    nodeByFqcn.put(cdd.node.getId(), cdd.node);
                    nodesBySimpleName.computeIfAbsent(cdd.node.getName(), k -> new ArrayList<>()).add(cdd.node);
                    graph.getNodes().add(cdd.node);
                }
                contexts.add(ctx);
            } catch (Exception e) {
                // Ignore unparseable files and continue best-effort
            }
        }

        // Pass 2: Resolve edges
        Set<ComponentEdge> edges = new LinkedHashSet<>();

        for (ParseContext ctx : contexts) {
            for (ClassDeclarationData cdd : ctx.classes) {
                String sourceId = cdd.node.getId();

                // 1. EXTENDS
                for (String rawExt : cdd.rawExtends) {
                    String targetId = resolveTargetId(rawExt, ctx, nodeByFqcn, nodesBySimpleName);
                    if (targetId != null && !targetId.equals(sourceId)) {
                        edges.add(new ComponentEdge(sourceId, targetId, RelationshipType.EXTENDS, "extends " + rawExt));
                    }
                }

                // 2. IMPLEMENTS
                for (String rawImp : cdd.rawImplements) {
                    String targetId = resolveTargetId(rawImp, ctx, nodeByFqcn, nodesBySimpleName);
                    if (targetId != null && !targetId.equals(sourceId)) {
                        edges.add(new ComponentEdge(sourceId, targetId, RelationshipType.IMPLEMENTS, "implements " + rawImp));
                    }
                }

                // 3. INJECTS vs USES (Fields)
                for (FieldInfo fi : cdd.node.getFields()) {
                    String targetId = resolveTargetId(fi.getType(), ctx, nodeByFqcn, nodesBySimpleName);
                    if (targetId != null && !targetId.equals(sourceId)) {
                        boolean isInjected = fi.getAnnotations().stream().anyMatch(a -> a.contains("Autowired") || a.contains("Inject"))
                                || (cdd.hasRequiredArgsConstructor && fi.isFinal())
                                || cdd.hasAllArgsConstructor
                                || cdd.hasSingleConstructorWithArgs;

                        RelationshipType relType = isInjected ? RelationshipType.INJECTS : RelationshipType.USES;
                        String desc = (isInjected ? "injects " : "field ") + fi.getName();
                        edges.add(new ComponentEdge(sourceId, targetId, relType, desc));
                    }
                }

                // 4. CALLS (Method calls)
                for (MethodCallInfo call : cdd.methodCalls) {
                    String targetType = null;
                    if (call.scope != null) {
                        // Check if scope is a field
                        targetType = cdd.fieldTypes.get(call.scope);
                        // Or check if scope is a known class name (static method call)
                        if (targetType == null && nodesBySimpleName.containsKey(call.scope)) {
                            targetType = call.scope;
                        }
                    }

                    if (targetType != null) {
                        String targetId = resolveTargetId(targetType, ctx, nodeByFqcn, nodesBySimpleName);
                        if (targetId != null && !targetId.equals(sourceId)) {
                            edges.add(new ComponentEdge(sourceId, targetId, RelationshipType.CALLS, "calls " + call.methodName + "()"));
                        }
                    }
                }
            }
        }

        graph.getEdges().addAll(edges);
        computeMetrics(graph, files.size());
        return graph;
    }

    private ClassDeclarationData extractClassData(TypeDeclaration<?> td, ParseContext ctx) {
        ClassDeclarationData cdd = new ClassDeclarationData();
        cdd.typeDeclaration = td;

        String simpleName = td.getNameAsString();
        String fqcn = ctx.packageName.isEmpty() ? simpleName : ctx.packageName + "." + simpleName;

        ComponentNode node = new ComponentNode(fqcn, simpleName, ctx.packageName, ComponentType.CLASS);
        node.setSourceFile(ctx.relativePath.toString());

        // Range / LOC
        if (td.getRange().isPresent()) {
            int lines = td.getRange().get().end.line - td.getRange().get().begin.line + 1;
            node.setLinesOfCode(lines);
        }

        // Annotations
        List<String> annotations = td.getAnnotations().stream()
                .map(AnnotationExpr::getNameAsString)
                .collect(Collectors.toList());
        node.setAnnotations(annotations);

        cdd.hasRequiredArgsConstructor = annotations.contains("RequiredArgsConstructor");
        cdd.hasAllArgsConstructor = annotations.contains("AllArgsConstructor");

        // Component type classification
        node.setType(classifyComponentType(td, annotations, ctx.packageName));

        // Constructor analysis
        List<ConstructorDeclaration> constructors = td.getConstructors();
        if (constructors.size() == 1 && !constructors.get(0).getParameters().isEmpty()) {
            cdd.hasSingleConstructorWithArgs = true;
        }

        // Inheritance & interfaces
        if (td instanceof ClassOrInterfaceDeclaration) {
            ClassOrInterfaceDeclaration cid = (ClassOrInterfaceDeclaration) td;
            for (ClassOrInterfaceType ext : cid.getExtendedTypes()) {
                String extName = ext.getNameAsString();
                cdd.rawExtends.add(extName);
                if (node.getSuperClass() == null) {
                    node.setSuperClass(extName);
                }
            }
            for (ClassOrInterfaceType imp : cid.getImplementedTypes()) {
                String impName = imp.getNameAsString();
                cdd.rawImplements.add(impName);
                node.getInterfaces().add(impName);
            }
        } else if (td instanceof RecordDeclaration) {
            RecordDeclaration rd = (RecordDeclaration) td;
            for (ClassOrInterfaceType imp : rd.getImplementedTypes()) {
                cdd.rawImplements.add(imp.getNameAsString());
                node.getInterfaces().add(imp.getNameAsString());
            }
        }

        // Fields
        for (FieldDeclaration fd : td.getFields()) {
            boolean isFinal = fd.isFinal();
            List<String> fieldAnnos = fd.getAnnotations().stream()
                    .map(AnnotationExpr::getNameAsString)
                    .collect(Collectors.toList());

            for (VariableDeclarator vd : fd.getVariables()) {
                String fieldName = vd.getNameAsString();
                String rawType = stripGenerics(vd.getType().asString());
                cdd.fieldNames.add(fieldName);
                cdd.fieldTypes.put(fieldName, rawType);

                FieldInfo fi = new FieldInfo(fieldName, rawType, isFinal, fieldAnnos);
                node.getFields().add(fi);
            }
        }

        // Methods & Calls
        for (MethodDeclaration md : td.getMethods()) {
            List<String> methodAnnos = md.getAnnotations().stream()
                    .map(AnnotationExpr::getNameAsString)
                    .collect(Collectors.toList());

            List<String> paramTypes = md.getParameters().stream()
                    .map(p -> stripGenerics(p.getType().asString()))
                    .collect(Collectors.toList());

            MethodInfo mi = new MethodInfo(md.getNameAsString(), md.getType().asString(), paramTypes, methodAnnos);
            node.getMethods().add(mi);

            // Scan method calls inside body
            md.findAll(MethodCallExpr.class).forEach(mce -> {
                MethodCallInfo mci = new MethodCallInfo();
                mci.methodName = mce.getNameAsString();
                if (mce.getScope().isPresent()) {
                    mci.scope = mce.getScope().get().toString();
                    if (mci.scope.startsWith("this.")) {
                        mci.scope = mci.scope.substring(5);
                    }
                }
                cdd.methodCalls.add(mci);
            });
        }

        cdd.node = node;
        return cdd;
    }

    private ComponentType classifyComponentType(TypeDeclaration<?> td, List<String> annotations, String packageName) {
        if (td.isEnumDeclaration()) {
            return ComponentType.ENUM;
        }
        if (td.isRecordDeclaration()) {
            return ComponentType.RECORD;
        }
        if (td instanceof ClassOrInterfaceDeclaration && ((ClassOrInterfaceDeclaration) td).isInterface()) {
            return ComponentType.INTERFACE;
        }

        for (String anno : annotations) {
            if (anno.equals("RestController") || anno.equals("Controller") || anno.contains("ControllerAdvice") || anno.contains("Endpoint")) {
                return ComponentType.CONTROLLER;
            }
            if (anno.equals("Service")) {
                return ComponentType.SERVICE;
            }
            if (anno.equals("Repository")) {
                return ComponentType.REPOSITORY;
            }
            if (anno.equals("Configuration") || anno.equals("SpringBootApplication")) {
                return ComponentType.CONFIGURATION;
            }
            if (anno.equals("Component")) {
                return ComponentType.COMPONENT;
            }
        }

        String lowerPkg = packageName.toLowerCase();
        String name = td.getNameAsString();
        if (lowerPkg.contains(".model") || lowerPkg.contains(".dto") || lowerPkg.contains(".entity") || lowerPkg.contains(".domain")) {
            return ComponentType.MODEL;
        }
        if (name.endsWith("Util") || name.endsWith("Utils") || name.endsWith("Helper")) {
            return ComponentType.UTILITY;
        }

        return ComponentType.CLASS;
    }

    private String resolveTargetId(String rawType, ParseContext ctx, Map<String, ComponentNode> nodeByFqcn, Map<String, List<ComponentNode>> nodesBySimpleName) {
        if (rawType == null || rawType.isEmpty()) {
            return null;
        }
        String simpleName = stripGenerics(rawType);
        if (simpleName.contains(".")) {
            return nodeByFqcn.containsKey(simpleName) ? simpleName : null;
        }

        // 1. Check same package
        String samePkgFqcn = ctx.packageName.isEmpty() ? simpleName : ctx.packageName + "." + simpleName;
        if (nodeByFqcn.containsKey(samePkgFqcn)) {
            return samePkgFqcn;
        }

        // 2. Check imports
        if (ctx.importMap.containsKey(simpleName)) {
            String importedFqcn = ctx.importMap.get(simpleName);
            if (nodeByFqcn.containsKey(importedFqcn)) {
                return importedFqcn;
            }
        }

        // 3. Any scanned class in project with that simple name
        List<ComponentNode> matches = nodesBySimpleName.get(simpleName);
        if (matches != null && !matches.isEmpty()) {
            return matches.get(0).getId();
        }

        return null;
    }

    private String stripGenerics(String type) {
        if (type == null) return "";
        int bracket = type.indexOf('<');
        if (bracket > 0) {
            type = type.substring(0, bracket);
        }
        int array = type.indexOf('[');
        if (array > 0) {
            type = type.substring(0, array);
        }
        return type.trim();
    }

    private void computeMetrics(ArchitectureGraph graph, int totalFiles) {
        ProjectMetrics metrics = graph.getMetrics();
        metrics.setTotalFiles(totalFiles);
        metrics.setTotalClasses((int) graph.getNodes().stream().filter(n -> n.getType() != ComponentType.INTERFACE).count());
        metrics.setTotalInterfaces((int) graph.getNodes().stream().filter(n -> n.getType() == ComponentType.INTERFACE).count());
        metrics.setTotalLinesOfCode(graph.getNodes().stream().mapToInt(ComponentNode::getLinesOfCode).sum());

        Set<String> packages = graph.getNodes().stream()
                .map(ComponentNode::getPackageName)
                .filter(p -> p != null && !p.isEmpty())
                .collect(Collectors.toSet());
        metrics.setPackageCount(packages.size());

        Map<String, Integer> typeCounts = new LinkedHashMap<>();
        for (ComponentType type : ComponentType.values()) {
            long count = graph.getNodes().stream().filter(n -> n.getType() == type).count();
            if (count > 0) {
                typeCounts.put(type.name(), (int) count);
            }
        }
        metrics.setTypeCounts(typeCounts);

        Map<String, Integer> relCounts = new LinkedHashMap<>();
        for (RelationshipType rel : RelationshipType.values()) {
            long count = graph.getEdges().stream().filter(e -> e.getType() == rel).count();
            if (count > 0) {
                relCounts.put(rel.name(), (int) count);
            }
        }
        metrics.setRelationshipCounts(relCounts);
    }
}
