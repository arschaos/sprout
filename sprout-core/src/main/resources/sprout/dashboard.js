(function () {
    const COLOR_MAP = {
        'CONTROLLER': '#3fb950',
        'SERVICE': '#58a6ff',
        'REPOSITORY': '#d29922',
        'CONFIGURATION': '#bc8cff',
        'COMPONENT': '#20b2aa',
        'MODEL': '#79c0ff',
        'UTILITY': '#ff7b72',
        'INTERFACE': '#a371f7',
        'RECORD': '#f0883e',
        'ENUM': '#e3b341',
        'CLASS': '#8b949e'
    };

    function initDashboard(graphData) {
        if (!graphData || !graphData.nodes) {
            console.error("No graph data available to render");
            return;
        }

        // Update header metrics
        const m = graphData.metrics || {};
        const titleSubtitle = document.getElementById('projectSubtitle');
        if (titleSubtitle) {
            titleSubtitle.textContent = (graphData.projectName || 'Project') + ' • Generated ' + (graphData.timestamp || '');
        }

        const metricsBar = document.getElementById('metricsBar');
        if (metricsBar) {
            metricsBar.innerHTML = `
                <div class="metric-badge">Classes: <strong>${m.totalClasses || 0}</strong></div>
                <div class="metric-badge">Interfaces: <strong>${m.totalInterfaces || 0}</strong></div>
                <div class="metric-badge">Lines of Code: <strong>${m.totalLinesOfCode || 0}</strong></div>
                <div class="metric-badge">Packages: <strong>${m.packageCount || 0}</strong></div>
                <div class="metric-badge">Relationships: <strong>${(graphData.edges || []).length}</strong></div>
            `;
        }

        const canvas = document.getElementById('graphCanvas');
        if (!canvas) return;
        const ctx = canvas.getContext('2d');

        let nodes = graphData.nodes.map((n, i) => ({
            ...n,
            x: 0,
            y: 0,
            vx: 0,
            vy: 0,
            radius: n.type === 'CONTROLLER' ? 24 : (n.type === 'SERVICE' ? 22 : 18)
        }));

        const nodeMap = new Map();
        nodes.forEach(n => nodeMap.set(n.id, n));

        const edges = (graphData.edges || []).map(e => ({
            ...e,
            sourceNode: nodeMap.get(e.source),
            targetNode: nodeMap.get(e.target)
        })).filter(e => e.sourceNode && e.targetNode);

        let activeFilters = new Set(Object.keys(m.typeCounts || {}));
        let searchQuery = '';
        let selectedNode = null;
        let hoveredNode = null;
        let transform = { x: 0, y: 0, k: 1 };
        let isDragging = false;
        let dragStart = { x: 0, y: 0 };
        let draggedNode = null;

        function resizeCanvas() {
            canvas.width = canvas.parentElement.clientWidth;
            canvas.height = canvas.parentElement.clientHeight;
        }
        window.addEventListener('resize', resizeCanvas);
        resizeCanvas();

        // Layer-based positioning
        const cx = canvas.width / 2;
        const cy = canvas.height / 2;
        nodes.forEach((n, i) => {
            let layerY = cy;
            if (n.type === 'CONTROLLER') layerY = cy - 200;
            else if (n.type === 'SERVICE') layerY = cy - 60;
            else if (n.type === 'INTERFACE') layerY = cy + 60;
            else if (n.type === 'CONFIGURATION') layerY = cy - 140;
            else if (n.type === 'REPOSITORY') layerY = cy + 160;
            else layerY = cy + 120 + (i % 3) * 60;

            const spreadX = ((i % 6) - 2.5) * 160;
            n.x = cx + spreadX + (Math.random() - 0.5) * 40;
            n.y = layerY + (Math.random() - 0.5) * 30;
        });

        // Setup filter buttons
        const filtersContainer = document.getElementById('filtersGroup');
        if (filtersContainer) {
            filtersContainer.innerHTML = '';
            Object.keys(m.typeCounts || {}).forEach(t => {
                const btn = document.createElement('button');
                btn.className = 'filter-btn active';
                btn.textContent = t + ' (' + m.typeCounts[t] + ')';
                btn.addEventListener('click', () => {
                    if (activeFilters.has(t)) {
                        activeFilters.delete(t);
                        btn.classList.remove('active');
                    } else {
                        activeFilters.add(t);
                        btn.classList.add('active');
                    }
                });
                filtersContainer.appendChild(btn);
            });
        }

        const searchInput = document.getElementById('searchInput');
        if (searchInput) {
            searchInput.addEventListener('input', e => {
                searchQuery = e.target.value.toLowerCase().trim();
            });
        }

        // Physics simulation
        function simulate() {
            for (let i = 0; i < nodes.length; i++) {
                for (let j = i + 1; j < nodes.length; j++) {
                    const dx = nodes[j].x - nodes[i].x;
                    const dy = nodes[j].y - nodes[i].y;
                    const dist = Math.sqrt(dx * dx + dy * dy) || 1;
                    if (dist < 180) {
                        const force = (180 - dist) / dist * 0.05;
                        nodes[i].vx -= dx * force;
                        nodes[i].vy -= dy * force;
                        nodes[j].vx += dx * force;
                        nodes[j].vy += dy * force;
                    }
                }
            }
            edges.forEach(e => {
                const dx = e.targetNode.x - e.sourceNode.x;
                const dy = e.targetNode.y - e.sourceNode.y;
                const dist = Math.sqrt(dx * dx + dy * dy) || 1;
                const targetDist = 140;
                const force = (dist - targetDist) * 0.003;
                e.sourceNode.vx += dx * force;
                e.sourceNode.vy += dy * force;
                e.targetNode.vx -= dx * force;
                e.targetNode.vy -= dy * force;
            });
            nodes.forEach(n => {
                if (n !== draggedNode) {
                    n.x += n.vx;
                    n.y += n.vy;
                    n.vx *= 0.85;
                    n.vy *= 0.85;
                }
            });
        }

        function draw() {
            simulate();
            ctx.save();
            ctx.clearRect(0, 0, canvas.width, canvas.height);
            ctx.translate(transform.x, transform.y);
            ctx.scale(transform.k, transform.k);

            // Draw edges
            edges.forEach(e => {
                if (!activeFilters.has(e.sourceNode.type) || !activeFilters.has(e.targetNode.type)) return;
                const isHighlighted = selectedNode && (e.sourceNode === selectedNode || e.targetNode === selectedNode);
                ctx.beginPath();
                ctx.moveTo(e.sourceNode.x, e.sourceNode.y);
                ctx.lineTo(e.targetNode.x, e.targetNode.y);
                ctx.strokeStyle = isHighlighted ? '#ffffff' : (e.type === 'INJECTS' ? '#3fb95088' : (e.type === 'IMPLEMENTS' ? '#a371f788' : '#30363d88'));
                ctx.lineWidth = isHighlighted ? 2.5 : 1.2;
                if (e.type === 'IMPLEMENTS') ctx.setLineDash([4, 4]);
                else ctx.setLineDash([]);
                ctx.stroke();
            });
            ctx.setLineDash([]);

            // Draw nodes
            nodes.forEach(n => {
                if (!activeFilters.has(n.type)) return;
                const matchesSearch = !searchQuery || n.name.toLowerCase().includes(searchQuery) || n.packageName.toLowerCase().includes(searchQuery);
                const isSelected = n === selectedNode;
                const isHovered = n === hoveredNode;
                const color = COLOR_MAP[n.type] || '#8b949e';

                ctx.beginPath();
                ctx.arc(n.x, n.y, n.radius, 0, Math.PI * 2);
                ctx.fillStyle = matchesSearch ? color : '#21262d';
                ctx.fill();
                ctx.lineWidth = isSelected ? 3 : (isHovered ? 2 : 1);
                ctx.strokeStyle = isSelected ? '#ffffff' : '#161b22';
                ctx.stroke();

                // Text label
                ctx.fillStyle = isSelected ? '#ffffff' : '#c9d1d9';
                ctx.font = (isSelected ? 'bold ' : '') + '12px sans-serif';
                ctx.textAlign = 'center';
                ctx.fillText(n.name, n.x, n.y + n.radius + 14);
            });

            ctx.restore();
            requestAnimationFrame(draw);
        }

        function findNodeAt(mx, my) {
            const x = (mx - transform.x) / transform.k;
            const y = (my - transform.y) / transform.k;
            for (let i = nodes.length - 1; i >= 0; i--) {
                const n = nodes[i];
                if (!activeFilters.has(n.type)) continue;
                const dist = Math.hypot(n.x - x, n.y - y);
                if (dist <= n.radius + 4) return n;
            }
            return null;
        }

        canvas.addEventListener('mousedown', e => {
            const rect = canvas.getBoundingClientRect();
            const mx = e.clientX - rect.left;
            const my = e.clientY - rect.top;
            const clickedNode = findNodeAt(mx, my);
            if (clickedNode) {
                draggedNode = clickedNode;
                selectNode(clickedNode);
            } else {
                isDragging = true;
                dragStart = { x: mx - transform.x, y: my - transform.y };
            }
        });

        canvas.addEventListener('mousemove', e => {
            const rect = canvas.getBoundingClientRect();
            const mx = e.clientX - rect.left;
            const my = e.clientY - rect.top;
            if (draggedNode) {
                draggedNode.x = (mx - transform.x) / transform.k;
                draggedNode.y = (my - transform.y) / transform.k;
            } else if (isDragging) {
                transform.x = mx - dragStart.x;
                transform.y = my - dragStart.y;
            } else {
                hoveredNode = findNodeAt(mx, my);
                canvas.style.cursor = hoveredNode ? 'pointer' : 'default';
            }
        });

        window.addEventListener('mouseup', () => {
            isDragging = false;
            draggedNode = null;
        });

        canvas.addEventListener('wheel', e => {
            e.preventDefault();
            const zoomFactor = e.deltaY < 0 ? 1.1 : 0.9;
            transform.k = Math.max(0.2, Math.min(3, transform.k * zoomFactor));
        });

        function selectNode(n) {
            selectedNode = n;
            const nameEl = document.getElementById('selectedName');
            const fqcnEl = document.getElementById('selectedFqcn');
            const metaEl = document.getElementById('selectedMeta');
            const locEl = document.getElementById('selectedLoc');
            const fileEl = document.getElementById('selectedFile');
            const tagsContainer = document.getElementById('selectedTags');

            if (nameEl) nameEl.textContent = n.name;
            if (fqcnEl) fqcnEl.textContent = n.id;
            if (metaEl) metaEl.style.display = 'block';
            if (locEl) locEl.innerHTML = '<strong>Lines of Code:</strong> ' + n.linesOfCode;
            if (fileEl) fileEl.innerHTML = '<strong>File:</strong> ' + (n.sourceFile || 'N/A');

            if (tagsContainer) {
                tagsContainer.innerHTML = '';
                const typeTag = document.createElement('span');
                typeTag.className = 'tag';
                typeTag.style.background = COLOR_MAP[n.type] || '#8b949e';
                typeTag.style.color = 'black';
                typeTag.textContent = n.type;
                tagsContainer.appendChild(typeTag);

                (n.annotations || []).forEach(a => {
                    const at = document.createElement('span');
                    at.className = 'tag';
                    at.style.background = '#21262d';
                    at.style.color = '#58a6ff';
                    at.textContent = '@' + a;
                    tagsContainer.appendChild(at);
                });
            }

            // Injections & Fields
            const injSec = document.getElementById('selectedInjections');
            const injDiv = document.getElementById('injectionsList');
            if (injDiv && injSec) {
                injDiv.innerHTML = '';
                if (n.fields && n.fields.length > 0) {
                    injSec.style.display = 'block';
                    n.fields.forEach(f => {
                        const d = document.createElement('div');
                        d.className = 'list-item';
                        d.textContent = (f.final ? 'final ' : '') + f.type + ' ' + f.name;
                        injDiv.appendChild(d);
                    });
                } else {
                    injSec.style.display = 'none';
                }
            }

            // Connections
            const connSec = document.getElementById('selectedEdges');
            const connDiv = document.getElementById('connectionsList');
            if (connDiv && connSec) {
                connDiv.innerHTML = '';
                const connectedEdges = edges.filter(e => e.source === n.id || e.target === n.id);
                if (connectedEdges.length > 0) {
                    connSec.style.display = 'block';
                    connectedEdges.forEach(e => {
                        const isOutgoing = e.source === n.id;
                        const other = isOutgoing ? e.targetNode.name : e.sourceNode.name;
                        const d = document.createElement('div');
                        d.className = 'list-item';
                        d.innerHTML = (isOutgoing ? '&#8594; ' : '&#8592; ') + '<strong>' + e.type + '</strong>: ' + other + ' <span style="color:#8b949e">(' + (e.description || '') + ')</span>';
                        connDiv.appendChild(d);
                    });
                } else {
                    connSec.style.display = 'none';
                }
            }

            // Methods
            const methSec = document.getElementById('selectedMethods');
            const methDiv = document.getElementById('methodsList');
            if (methDiv && methSec) {
                methDiv.innerHTML = '';
                if (n.methods && n.methods.length > 0) {
                    methSec.style.display = 'block';
                    n.methods.forEach(m => {
                        const d = document.createElement('div');
                        d.className = 'list-item';
                        d.textContent = m.returnType + ' ' + m.name + '(' + (m.parameterTypes || []).join(', ') + ')';
                        methDiv.appendChild(d);
                    });
                } else {
                    methSec.style.display = 'none';
                }
            }
        }

        draw();
    }

    // Auto-initialize when DOM is ready
    function start() {
        if (window.SPROUT_DATA) {
            initDashboard(window.SPROUT_DATA);
        } else {
            // Attempt to fetch from REST endpoint
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
})();
