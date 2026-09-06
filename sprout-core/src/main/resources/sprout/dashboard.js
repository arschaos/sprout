/**
 * Sprout Architecture Visualizer - Interactive Graph
 */
let cy = null;
let graphData = null;

const COLOR_MAP = {
    'CONTROLLER':    { bg: '#1e3a8a', border: '#3b82f6', text: '#93c5fd' },
    'SERVICE':       { bg: '#064e3b', border: '#10b981', text: '#6ee7b7' },
    'REPOSITORY':    { bg: '#78350f', border: '#f59e0b', text: '#fcd34d' },
    'CONFIGURATION': { bg: '#831843', border: '#ec4899', text: '#f472b6' },
    'COMPONENT':     { bg: '#134e4a', border: '#14b8a6', text: '#5eead4' },
    'MODEL':         { bg: '#4c1d95', border: '#8b5cf6', text: '#c4b5fd' },
    'UTILITY':       { bg: '#7f1d1d', border: '#ef4444', text: '#fca5a5' },
    'INTERFACE':     { bg: '#3b0764', border: '#a371f7', text: '#d8b4fe' },
    'RECORD':        { bg: '#7c2d12', border: '#f97316', text: '#fdba74' },
    'ENUM':          { bg: '#713f12', border: '#eab308', text: '#fde047' },
    'CLASS':         { bg: '#1e293b', border: '#64748b', text: '#cbd5e1' }
};

let activeTypes = new Set();

function initDashboard(data) {
    if (!data || !data.nodes) {
        console.error("No graph data available to render");
        return;
    }
    graphData = data;

    const titleSubtitle = document.getElementById('projectSubtitle');
    if (titleSubtitle) {
        titleSubtitle.textContent = (data.projectName || 'Project') + ' • Generated ' + (data.timestamp || '');
    }

    const m = data.metrics || {};
    const classesEl = document.getElementById('metric-classes');
    const locEl = document.getElementById('metric-loc');
    const nodeCountEl = document.getElementById('node-count');
    const edgeCountEl = document.getElementById('edge-count');
    if (classesEl) classesEl.innerText = `Classes: ${m.totalClasses || 0}`;
    if (locEl) locEl.innerText = `LOC: ${m.totalLinesOfCode || 0}`;
    if (nodeCountEl) nodeCountEl.innerText = `Nodes: ${(data.nodes || []).length}`;
    if (edgeCountEl) edgeCountEl.innerText = `Edges: ${(data.edges || []).length}`;

    renderFilters(data);
    renderLegendComponents(data);
    renderCytoscape(data);
}

function renderFilters(data) {
    const container = document.getElementById('filtersGroup');
    if (!container) return;
    container.innerHTML = '';

    const counts = {};
    (data.nodes || []).forEach(n => {
        const t = n.type || 'CLASS';
        counts[t] = (counts[t] || 0) + 1;
    });

    Object.keys(counts).forEach(type => {
        activeTypes.add(type);
        const color = (COLOR_MAP[type] || COLOR_MAP.CLASS).border;
        const chip = document.createElement('div');
        chip.className = 'filter-chip active';
        chip.innerHTML = `<span class="color-dot" style="background:${color};"></span> ${type} (${counts[type]})`;
        chip.onclick = () => toggleTypeFilter(type, chip);
        container.appendChild(chip);
    });
}

function renderCytoscape(data) {
    const container = document.getElementById('cy');
    if (!container) return;

    if (typeof cytoscape === 'undefined') {
        container.innerHTML = '<div style="color:#ef4444; padding:30px; font-size:14px;">Error: Cytoscape.js could not be loaded. Please ensure internet access to CDNs is available.</div>';
        return;
    }

    const elements = [];
    const nodeIds = new Set();

    // 1. Build package hierarchy compound nodes so classes visually group by package
    const packageSet = new Set();
    (data.nodes || []).forEach(n => {
        if (n.packageName && n.packageName.trim().length > 0) {
            packageSet.add(n.packageName);
        }
    });

    // Collect all ancestor package paths (e.g., "com", "com.eclark", "com.eclark.task_aggregator_api")
    const allAncestors = new Set();
    packageSet.forEach(pkg => {
        const parts = pkg.split('.');
        let curr = '';
        for (let i = 0; i < parts.length; i++) {
            curr = curr ? curr + '.' + parts[i] : parts[i];
            allAncestors.add(curr);
        }
    });

    // Add all package compound nodes to elements
    allAncestors.forEach(pkg => {
        nodeIds.add(pkg);
        const parts = pkg.split('.');
        const parent = pkg.includes('.') ? pkg.substring(0, pkg.lastIndexOf('.')) : undefined;
        elements.push({
            group: 'nodes',
            data: {
                id: pkg,
                label: parts[parts.length - 1],
                type: 'PACKAGE',
                parent: parent
            },
            classes: 'package-node'
        });
    });

    // 2. Build component nodes
    (data.nodes || []).forEach(n => {
        const id = n.id;
        if (!id || nodeIds.has(id)) return;
        nodeIds.add(id);

        const type = n.type || 'CLASS';
        elements.push({
            group: 'nodes',
            data: {
                id: id,
                label: n.name || id,
                type: type,
                packageName: n.packageName,
                parent: (n.packageName && n.packageName.trim().length > 0) ? n.packageName : undefined,
                methods: n.methods || [],
                fields: n.fields || [],
                annotations: n.annotations || [],
                linesOfCode: n.linesOfCode,
                sourceFile: n.sourceFile
            },
            classes: `node-${type.toLowerCase()}`
        });
    });

    // 3. Sanitize parent links (prevent Cytoscape from throwing on a dangling parent)
    elements.forEach(el => {
        if (el.group === 'nodes' && el.data.parent && !nodeIds.has(el.data.parent)) {
            el.data.parent = undefined;
        }
    });

    // 4. Build edges (only where both endpoints made it into the graph)
    (data.edges || []).forEach((e, idx) => {
        const src = e.source;
        const tgt = e.target;
        if (nodeIds.has(src) && nodeIds.has(tgt)) {
            elements.push({
                group: 'edges',
                data: {
                    id: `e_${idx}`,
                    source: src,
                    target: tgt,
                    type: e.type,
                    description: e.description || ''
                },
                classes: `edge-${(e.type || 'uses').toLowerCase()}`
            });
        }
    });

    const nodeStyles = Object.keys(COLOR_MAP).map(type => ({
        selector: `.node-${type.toLowerCase()}`,
        style: {
            'background-color': COLOR_MAP[type].bg,
            'border-color': COLOR_MAP[type].border,
            'color': COLOR_MAP[type].text
        }
    }));

    try {
        cy = cytoscape({
            container: container,
            elements: elements,
            minZoom: 0.05,
            maxZoom: 4,
            style: [
                {
                    selector: 'node',
                    style: {
                        'label': 'data(label)',
                        'color': '#f3f4f6',
                        'font-size': '11px',
                        'font-family': 'Inter, sans-serif',
                        'text-valign': 'center',
                        'text-halign': 'center',
                        'background-color': '#1f2937',
                        'border-width': 1.5,
                        'border-color': '#4b5563',
                        'shape': 'round-rectangle',
                        'width': 'label',
                        'padding': '6px'
                    }
                },
                {
                    selector: ':parent',
                    style: {
                        'background-color': '#0f172a',
                        'background-opacity': 0.35,
                        'border-color': '#334155',
                        'border-style': 'dashed',
                        'border-width': 1.5,
                        'font-size': '11px',
                        'font-weight': 'bold',
                        'text-valign': 'top',
                        'text-halign': 'center',
                        'color': '#64748b',
                        'padding': '8px'
                    }
                },
                ...nodeStyles,
                {
                    selector: 'edge',
                    style: {
                        'width': 1.5,
                        'line-color': '#64748b',
                        'target-arrow-color': '#64748b',
                        'target-arrow-shape': 'triangle',
                        'curve-style': 'bezier',
                        'arrow-scale': 0.85,
                        'opacity': 0.8
                    }
                },
                {
                    selector: '.edge-injects',
                    style: { 'line-color': '#6366f1', 'target-arrow-color': '#6366f1', 'line-style': 'solid', 'width': 2, 'opacity': 0.95 }
                },
                {
                    selector: '.edge-extends',
                    style: { 'line-color': '#10b981', 'target-arrow-color': '#10b981', 'line-style': 'solid', 'target-arrow-shape': 'triangle-backcurve', 'width': 1.5 }
                },
                {
                    selector: '.edge-implements',
                    style: { 'line-color': '#06b6d4', 'target-arrow-color': '#06b6d4', 'line-style': 'dashed', 'target-arrow-shape': 'triangle-backcurve', 'width': 1.5 }
                },
                {
                    selector: '.edge-calls',
                    style: { 'line-color': '#f97316', 'target-arrow-color': '#f97316', 'line-style': 'dotted', 'width': 1.5, 'opacity': 0.9 }
                },
                {
                    selector: '.edge-uses',
                    style: { 'line-color': '#64748b', 'target-arrow-color': '#64748b', 'line-style': 'solid', 'width': 1.5 }
                },
                {
                    selector: 'node:selected',
                    style: {
                        'border-color': '#ffffff',
                        'border-width': 3,
                        'shadow-blur': 15,
                        'shadow-color': '#6366f1',
                        'shadow-opacity': 0.8
                    }
                }
            ]
        });

        cy.on('tap', 'node', function (evt) {
            const node = evt.target;
            if (node.data('type') !== 'PACKAGE') {
                showDetails(node.data());
            }
        });

        cy.on('tap', 'edge', function (evt) {
            showEdgeDetails(evt.target.data());
        });

        cy.on('tap', function (evt) {
            if (evt.target === cy) {
                hideDetails();
                resetHighlight();
            }
        });

        window.addEventListener('resize', () => {
            if (cy) {
                cy.resize();
                cy.fit(undefined, 20);
            }
        });

        // Give the flex container a frame to resolve its final size before laying out
        setTimeout(() => {
            if (cy) {
                cy.resize();
                runLayout();
            }
        }, 50);
    } catch (e) {
        console.error('Cytoscape initialization error:', e);
    }
}

function runLayout() {
    if (!cy) return;
    try {
        const layout = cy.layout({
            name: 'fcose',
            quality: 'proof',
            randomize: true,
            animate: true,
            animationDuration: 450,
            fit: true,
            padding: 20,
            nodeDimensionsIncludeLabels: true,
            uniformNodeDimensions: false,
            packComponents: true,
            nodeRepulsion: node => 2200,
            idealEdgeLength: edge => 45,
            nodeSeparation: 30,
            gravity: 0.35,
            gravityRangeCompound: 1.5,
            gravityCompound: 1.0,
            gravityRange: 3.8
        });
        layout.on('layoutstop', () => cy.fit(undefined, 20));
        layout.run();
    } catch (e) {
        console.warn('fcose layout failed or not registered, falling back to cose:', e);
        try {
            const fallbackLayout = cy.layout({
                name: 'cose',
                animate: true,
                animationDuration: 400,
                fit: true,
                padding: 20,
                randomize: true,
                idealEdgeLength: 45,
                nodeRepulsion: 2200,
                nodeOverlap: 4,
                componentSpacing: 30
            });
            fallbackLayout.on('layoutstop', () => cy.fit(undefined, 20));
            fallbackLayout.run();
        } catch (ignored) {}
    }
}

function showDetails(data) {
    const welcome = document.getElementById('welcome-msg');
    const details = document.getElementById('node-details');
    const edgeDetails = document.getElementById('edge-details');
    if (welcome) welcome.style.display = 'none';
    if (edgeDetails) edgeDetails.style.display = 'none';
    if (details) details.style.display = 'block';

    const typeEl = document.getElementById('detail-type');
    const nameEl = document.getElementById('detail-name');
    const pkgEl = document.getElementById('detail-pkg');

    if (typeEl) {
        typeEl.innerText = data.type || 'CLASS';
        const color = (COLOR_MAP[data.type] || COLOR_MAP.CLASS).border;
        typeEl.style.borderColor = color;
        typeEl.style.color = color;
    }
    if (nameEl) nameEl.innerText = data.label || data.id;
    if (pkgEl) pkgEl.innerText = data.id || data.packageName || '';

    // Fields
    const fieldsBox = document.getElementById('fields-box');
    const fieldList = document.getElementById('field-list');
    if (fieldsBox && fieldList) {
        if (data.fields && data.fields.length > 0) {
            fieldsBox.style.display = 'block';
            fieldList.innerHTML = data.fields.map(f => `
                <li>${f.final ? '<span style="color:#6ee7b7;">final</span> ' : ''}<span style="color:#9ca3af;">${f.type}</span> ${f.name}</li>
            `).join('');
        } else {
            fieldsBox.style.display = 'none';
        }
    }

    // Methods
    const methodsBox = document.getElementById('methods-box');
    const methodList = document.getElementById('method-list');
    if (methodsBox && methodList) {
        if (data.methods && data.methods.length > 0) {
            methodsBox.style.display = 'block';
            methodList.innerHTML = data.methods.map(m => `
                <li><span style="color:#34d399;">${m.returnType}</span> ${m.name}(${(m.parameterTypes || []).join(', ')})</li>
            `).join('');
        } else {
            methodsBox.style.display = 'none';
        }
    }

    // Connections (both directions)
    const connBox = document.getElementById('connections-box');
    const connList = document.getElementById('connection-list');
    if (connBox && connList) {
        const targetNode = cy ? cy.getElementById(data.id) : null;
        const outEdges = targetNode && targetNode.length > 0 ? targetNode.outgoers('edge') : [];
        const inEdges = targetNode && targetNode.length > 0 ? targetNode.incomers('edge') : [];
        if (outEdges.length > 0 || inEdges.length > 0) {
            connBox.style.display = 'block';
            let html = '';
            outEdges.forEach(e => {
                const other = e.target().data('label') || e.target().id();
                html += `<li>&#8594; <strong>${e.data('type')}</strong> ${other}</li>`;
            });
            inEdges.forEach(e => {
                const other = e.source().data('label') || e.source().id();
                html += `<li>&#8592; <strong>${e.data('type')}</strong> ${other}</li>`;
            });
            connList.innerHTML = html;
        } else {
            connBox.style.display = 'none';
        }
    }

    const inspectBtn = document.querySelectorAll('.tab-btn')[0];
    if (inspectBtn) switchTab('inspect', inspectBtn);
}

const ARROW_INFO_MAP = {
    'INJECTS':    { style: 'Solid Indigo line', meaning: 'Dependency injection via Spring @Autowired or constructor' },
    'EXTENDS':    { style: 'Solid Green line', meaning: 'Class inheritance (extends superclass)' },
    'IMPLEMENTS': { style: 'Dashed Cyan line', meaning: 'Interface realization (implements interface)' },
    'CALLS':      { style: 'Dotted Orange line', meaning: 'Method invocation detected in code' },
    'USES':       { style: 'Solid Gray line', meaning: 'Field reference or general association' }
};

function showEdgeDetails(data) {
    const welcome = document.getElementById('welcome-msg');
    const details = document.getElementById('node-details');
    const edgeDetails = document.getElementById('edge-details');
    if (welcome) welcome.style.display = 'none';
    if (details) details.style.display = 'none';
    if (edgeDetails) edgeDetails.style.display = 'block';

    const edgeType = (data.type || 'USES').toUpperCase();
    const info = ARROW_INFO_MAP[edgeType] || ARROW_INFO_MAP['USES'];

    const typeEl = document.getElementById('edge-detail-type');
    const nameEl = document.getElementById('edge-detail-name');
    const styleEl = document.getElementById('edge-detail-style');
    const descEl = document.getElementById('edge-detail-desc');

    if (typeEl) {
        typeEl.innerText = edgeType;
        const colorMap = {
            'INJECTS': '#6366f1',
            'EXTENDS': '#10b981',
            'IMPLEMENTS': '#06b6d4',
            'CALLS': '#f97316',
            'USES': '#64748b'
        };
        const c = colorMap[edgeType] || '#64748b';
        typeEl.style.borderColor = c;
        typeEl.style.color = c;
    }

    if (nameEl) {
        const src = data.source ? data.source.split('.').pop() : '';
        const tgt = data.target ? data.target.split('.').pop() : '';
        nameEl.innerText = `${src} → ${tgt}`;
    }

    if (styleEl) {
        styleEl.innerText = `${info.style} • ${info.meaning}`;
    }

    if (descEl) {
        descEl.innerText = data.description || `${data.source} ${edgeType.toLowerCase()} ${data.target}`;
    }

    const inspectBtn = document.querySelectorAll('.tab-btn')[0];
    if (inspectBtn) switchTab('inspect', inspectBtn);
}

function hideDetails() {
    const welcome = document.getElementById('welcome-msg');
    const details = document.getElementById('node-details');
    const edgeDetails = document.getElementById('edge-details');
    if (welcome) welcome.style.display = 'block';
    if (details) details.style.display = 'none';
    if (edgeDetails) edgeDetails.style.display = 'none';
}

let highlightedEdgeType = null;

function highlightEdgeType(type) {
    if (!cy) return;
    const normType = (type || '').toUpperCase();
    if (highlightedEdgeType === normType) {
        resetHighlight();
        return;
    }
    highlightedEdgeType = normType;
    document.querySelectorAll('.legend-card').forEach(r => r.classList.remove('active-legend-card'));

    const tabCard = document.getElementById(`tab-legend-card-${normType.toLowerCase()}`);
    if (tabCard) tabCard.classList.add('active-legend-card');

    const matchingEdges = cy.edges().filter(e => (e.data('type') || '').toUpperCase() === normType);
    const connectedNodeIds = new Set();
    matchingEdges.forEach(e => {
        connectedNodeIds.add(e.source().id());
        connectedNodeIds.add(e.target().id());
    });

    cy.edges().forEach(e => {
        if ((e.data('type') || '').toUpperCase() === normType) {
            e.style('opacity', 1);
            e.style('width', 2.8);
        } else {
            e.style('opacity', 0.1);
            e.style('width', 1.5);
        }
    });

    cy.nodes().forEach(n => {
        if (n.data('type') === 'PACKAGE') {
            n.style('opacity', 0.9);
        } else if (connectedNodeIds.has(n.id())) {
            n.style('opacity', 1);
        } else {
            n.style('opacity', 0.15);
        }
    });
}

function resetHighlight() {
    highlightedEdgeType = null;
    document.querySelectorAll('.legend-card').forEach(r => r.classList.remove('active-legend-card'));
    if (!cy) return;
    cy.edges().forEach(e => {
        const edgeType = (e.data('type') || 'uses').toUpperCase();
        e.style('opacity', edgeType === 'INJECTS' ? 0.95 : 0.8);
        e.style('width', edgeType === 'INJECTS' ? 2 : 1.5);
    });
    cy.nodes().style('opacity', 1);
}

function renderLegendComponents(data) {
    const container = document.getElementById('legendComponentTypes');
    if (!container) return;
    container.innerHTML = '';

    const types = Object.keys(COLOR_MAP);
    types.forEach(type => {
        const info = COLOR_MAP[type];
        const chip = document.createElement('div');
        chip.className = 'legend-comp-chip';
        chip.innerHTML = `<span class="color-dot" style="background:${info.border};"></span> <span>${type}</span>`;
        container.appendChild(chip);
    });
}

function searchNodes() {
    const searchInput = document.getElementById('search-input');
    if (!searchInput || !cy) return;
    const query = searchInput.value.toLowerCase().trim();

    if (query === '') {
        cy.nodes().style('opacity', 1);
        cy.edges().style('opacity', 0.75);
        return;
    }

    cy.nodes().forEach(node => {
        const label = (node.data('label') || '').toLowerCase();
        const id = (node.data('id') || '').toLowerCase();
        if (label.includes(query) || id.includes(query)) {
            node.style('opacity', 1);
            let parent = node.parent();
            while (parent && parent.length > 0) {
                parent.style('opacity', 1);
                parent = parent.parent();
            }
        } else {
            node.style('opacity', 0.12);
        }
    });
}

function toggleTypeFilter(type, chip) {
    if (activeTypes.has(type)) {
        activeTypes.delete(type);
        if (chip) chip.classList.remove('active');
    } else {
        activeTypes.add(type);
        if (chip) chip.classList.add('active');
    }

    if (!cy) return;
    cy.nodes().forEach(node => {
        const nodeType = node.data('type');
        if (nodeType === 'PACKAGE' || activeTypes.has(nodeType)) {
            node.style('display', 'element');
        } else {
            node.style('display', 'none');
        }
    });
}

function switchTab(tab, btn) {
    document.querySelectorAll('.tab-btn').forEach(b => b.classList.remove('active'));
    document.querySelectorAll('.tab-content').forEach(c => c.style.display = 'none');

    if (btn) {
        btn.classList.add('active');
    } else {
        const targetBtn = Array.from(document.querySelectorAll('.tab-btn')).find(b => b.innerText.toLowerCase().includes(tab.toLowerCase()));
        if (targetBtn) targetBtn.classList.add('active');
    }

    const targetContent = document.getElementById(`tab-${tab}`);
    if (targetContent) targetContent.style.display = 'flex';
}

// Auto-initialize when DOM is ready
function start() {
    if (window.SPROUT_DATA) {
        initDashboard(window.SPROUT_DATA);
    } else {
        fetch('/api/graph')
            .then(r => r.json())
            .then(data => initDashboard(data))
            .catch(err => console.warn('Could not load /api/graph automatically:', err));
    }
}

if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', start);
} else {
    start();
}
