window.initBerekeningGraph = function(element, graph, glossary, $scope, selectedNode, hoveredNode) {

  var units = "inputs";

  var highlightSelected = undefined;

  var margin = {top: 20, right: 10, bottom: 20, left: 10},
    width = element.offsetWidth - margin.left - margin.right,
    height = element.offsetHeight - margin.top - margin.bottom;

  var formatNumber = d3.format(",.0f"),    // zero decimal places
    format = function (d) {
      return formatNumber(d) + " " + units;
    },
    color = d3.scale.category20();

  // Clear the element
  d3.select(element).select("svg").remove();

  new ResizeSensor(element, resize);

  // append the svg canvas to the page
  var svg = d3.select(element).append("svg")
    .attr("width", width + margin.left + margin.right)
    .attr("height", height + margin.top + margin.bottom)
    .classed("normal", true);

  var graphPadded = svg.append("g")
    .attr("transform",
      "translate(" + margin.left + "," + margin.top + ")");

  // Set the sankey diagram properties
  var sankey = d3.sankey()
    .nodeWidth(15)
    .nodePadding(10)
    .size([width, height]);

  var path = sankey.link();

  // load the data
    sankey
      .nodes(graph.nodes)
      .links(graph.edges)
      .layout(32);

    var link = graphPadded.append("g").selectAll(".link")
      .data(graph.edges)
      .enter().append("path")
      .attr("class", function(d) {
        return "link " + d.edgeType + "-link"
      })
      .attr("id", function (d, i) {
        return d.htmlId = "link-" + i;
      })
      .attr("d", path)
      .style("stroke-width", function (d) {
        return Math.max(1, d.dy);
      })
      .sort(function (a, b) {
        return b.dy - a.dy;
      });

    link.append("title")
      .text(function (d) {
        return d.source.name + " → " + d.target.name + "\n" + format(d.value);
      });

    var node = graphPadded.append("g").selectAll(".node")
      .data(graph.nodes)
      .enter().append("g")
      .classed("node", true)
      .attr("transform", function (d) {
        return "translate(" + d.x + "," + d.y + ")";
      })
      .on("mouseenter", highlight_node_links)
      .on("mouseleave", dehighlight_node_links)
      .on("mouseup", select_clicked_node)
      .attr("id", function (d, i) {
        return d.htmlId = "node-" + i;
      })
      .call(d3.behavior.drag()
        .origin(function (d) {
          return d;
        })
        .on("dragstart", function () {
          this.parentNode.appendChild(this);
        })
        .on("drag", dragmove));

    node.append("rect")
      .attr("height", function (d) {
        return d.dy;
      })
      .attr("width", sankey.nodeWidth())
      .style("fill", function (d) {
        return d.color = color(d.name.replace(/ .*/, ""));
      })
      .style("stroke", function (d) {
        return d3.rgb(d.color).darker(2);
      })
      .append("title")
      .html(buildNodeToolTip);

    node.append("text")
      .attr("x", -6)
      .attr("y", function (d) {
        return d.dy / 2;
      })
      .attr("dy", ".35em")
      .attr("text-anchor", "end")
      .attr("transform", null)
      .classed("node-name", true)
      .text(function (d) {
        return d.name;
      })
      .filter(function (d) {
        return d.x < width / 2;
      })
      .attr("x", 6 + sankey.nodeWidth())
      .attr("text-anchor", "start");

    function dragmove(d) {
      d3.select(this).attr("transform", "translate(" + d.x + "," + (d.y = Math.max(0, Math.min(height - d.dy, d3.event.y))) + ")");
      sankey.relayout();
      link.attr("d", path);
    }

  // --- Functions for highlighting nodes etc
  function select_clicked_node(node) {
    if (highlightSelected === node) {
      updateNodeSelection(node, {"selected": false}, {"filtered": false});
      highlightSelected = undefined;
      $scope.$apply(function() {
        $scope[selectedNode] = undefined;
      });
    }
    else if (highlightSelected !== undefined) {
      updateNodeSelection(highlightSelected, {"selected": false}, {"filtered": false});
      updateNodeSelection(node, {"selected": true}, {"filtered": true});
      highlightSelected = node;
      $scope.$apply(function() {
        $scope[selectedNode] = node.name;
      });
    }
    else {
      updateNodeSelection(node, {"selected": true}, {"filtered": true});
      highlightSelected = node;
      $scope.$apply(function() {
        $scope[selectedNode] = node.name;
      });
    }
  }

  function dehighlight_node_links(node) {
    if (highlightSelected === undefined) {
      updateNodeSelection(node, {"selected": false}, {"filtered": false});
      $scope.$apply(function() {
        $scope[hoveredNode] = undefined;
      });
    }
  }

  function updateNodeSelection(node, classMutator, graphClass) {
    change_node_links(node, classMutator);
    d3.select('#' + node.htmlId).classed(classMutator);
    svg.classed(graphClass);
  }

  function highlight_node_links(node) {
    if (highlightSelected === undefined) {
      updateNodeSelection(node, {"selected": true}, {"filtered": true});
      $scope.$apply(function() {
        $scope[hoveredNode] = node.name;
      });
    }
  }

  function change_node_links(node, classMutator) {

    var remainingNodes = [],
      nextNodes = [];

    var traverse = [{
      linkType: "sourceLinks",
      nodeType: "target"
    }, {
      linkType: "targetLinks",
      nodeType: "source"
    }];

    traverse.forEach(function (step) {
      node[step.linkType].forEach(function (link) {
        remainingNodes.push(link[step.nodeType]);
        highlight_link(link.htmlId, classMutator);
      });

      while (remainingNodes.length) {
        nextNodes = [];
        remainingNodes.forEach(function (node) {
          node[step.linkType].forEach(function (link) {
            nextNodes.push(link[step.nodeType]);
            highlight_link(link.htmlId, classMutator);
          });
        });
        remainingNodes = nextNodes;
      }
    });
  }

  function highlight_link(id, classMutator) {
    d3.select("#" + id).classed(classMutator);
  }

  // --- Tooltips

  function buildNodeToolTip(d) {
    var description = (glossary.hasOwnProperty(d.name)) ? glossary[d.name].description : '--';
    return d.name + " (" + description + ")" + "\n" + format(d.value) + " - " + d.nodeType + "\n\n" + d.description;
  }

  // --- Resize

  function resize() {
    // var newWidth = element.offsetWidth - margin.left - margin.right,
    //   newHeight = element.offsetHeight - margin.top - margin.bottom;
    //
    // svg.attr("width", newWidth + margin.left + margin.right)
    //   .attr("height", newHeight + margin.top + margin.bottom);
    //
    // // Set the sankey diagram properties
    // sankey.size([newWidth, newHeight])
    //   .layout(32);
    //
    // link.data(graph.links).attr("d", path);
    //
    // node.data(graph.nodes).attr("transform", function (d) {
    //   return "translate(" + d.x + "," + d.y + ")";
    // });
  }
}
