
import {knoxClass} from "./knox.js";

// The target class observes an SVG element on the page, and
// provides methods for setting and clearing graph data. A variable
// called 'targets' holds all the svg rendering targets on the page.
export default class Target{

  constructor(id){
    this.layout = null;
    this.id = id;
    $(id).width($(id).parent().width());
    $(id).height($(id).parent().height());
  }

  clear() {
    d3.select(this.id).selectAll("*").remove();
    delete this.layout;
  }

  removeGraph() {
    delete this.layout;
  }

  expandToFillParent() {
    var width = $(this.id).parent().width();
    var height = $(this.id).parent().height();
    if (this.layout) {
      this.layout.size([width, height]);
      this.layout.start();
    }
    $(this.id).width($(this.id).parent().width());
    $(this.id).height($(this.id).parent().height());
  }

  setGraph(graph) {
    var zoom = d3.behavior.zoom()
      .scaleExtent([1, 10])
      .on("zoom", () => {
        svg.attr("transform", "translate(" + d3.event.translate + ")scale(" + d3.event.scale + ")");
      });

    var svg = d3.select(this.id).call(zoom).append("svg:g");
    svg.append("defs").append("marker")
      .attr("id", "endArrow")
      .attr("viewBox", "0 -5 10 10")
      .attr("refX", 6)
      .attr("markerWidth", 6)
      .attr("markerHeight", 6)
      .attr("orient", "auto")
      .append("path")
      .attr("d", "M0,-5L10,0L0,5")
      .attr("fill", "#999")
      .attr("opacity", "0.5");
      var force = (this.layout = d3.layout.force());
      force.drag().on("dragstart", () => {
        d3.event.sourceEvent.stopPropagation();
      });
      force.charge(-400).linkDistance(100);
      force.nodes(graph.nodes).links(graph.links).size([
        $(this.id).parent().width(), $(this.id).parent().height()
      ]).start();

    var linksEnter = svg.selectAll(".link")
      .data(graph.links)
      .enter();

    var links = linksEnter.append("path")
      .attr("class", "link");

    var nodesEnter = svg.selectAll(".node")
      .data(graph.nodes)
      .enter();

    var circles = nodesEnter.append("circle")
      .attr("class", function(d) {
        if (d.nodeTypes.length === 0) {
          return "node";
        } else if (d.nodeTypes.indexOf("start") >= 0) {
          return "start-node";
        } else if (d.nodeTypes.indexOf("accept") >= 0) {
          return "accept-node";
        }
      })
      .attr("r", 7).call(force.drag);

    const sbolImgSize = 50;

    var images = linksEnter.append("svg:image")
      .attr("height", sbolImgSize)
      .attr("width", sbolImgSize)
      .attr("class", "sboltip")
      .attr("title", (d) => {
        if (d.hasOwnProperty("componentIDs")) {
          return d["componentIDs"].toString();
        }
      })
      .attr("xlink:href", (d) => {
        if (d.hasOwnProperty("componentRoles")) {
          const sbolpath = "./img/sbol/";
          if (d["componentRoles"].length > 0) {
            var role = d["componentRoles"][0];
            switch (role) {
              case "promoter":
              case "terminator":
              case "CDS":
              case "restriction_enzyme_assembly_scar":
              case "restriction_enzyme_recognition_site":
              case "protein_stability_element":
              case "blunt_end_restriction_enzyme_cleavage_site":
              case "ribonuclease_site":
              case "restriction_enzyme_five_prime_single_strand_overhang":
              case "ribosome_entry_site":
              case "five_prime_sticky_end_restriction_enzyme_cleavage_site":
              case "RNA_stability_element":
              case "ribozyme":
              case "insulator":
              case "signature":
              case "operator":
              case "origin_of_replication":
              case "restriction_enzyme_three_prime_single_strand_overhang":
              case "primer_binding_site":
              case "three_prime_sticky_end_restriction_enzyme_cleavage_site":
              case "protease_site":
                return sbolpath + role + ".svg";
              default:
                return sbolpath + "user_defined.svg";
            }
          }
        }
        return "";
    })

    $('.sboltip').tooltipster({
      theme: 'tooltipster-shadow'
    });

    force.on("tick", function () {
      links.attr('d', function(d) {
        var deltaX = d.target.x - d.source.x,
        deltaY = d.target.y - d.source.y,
        dist = Math.sqrt(deltaX * deltaX + deltaY * deltaY),
        normX = deltaX / dist,
        normY = deltaY / dist,
        sourcePadding = 12,
        targetPadding = 12,
        sourceX = d.source.x + normX * sourcePadding,
        sourceY = d.source.y + normY * sourcePadding,
        targetX = d.target.x - normX * targetPadding,
        targetY = d.target.y - normY * targetPadding;
        return 'M' + sourceX + ',' + sourceY + 'L' + targetX + ',' + targetY;
      });

      circles.attr("cx", function (d) {
        return d.x;
      })
      .attr("cy", function (d) {
        return d.y;
      });

      images.attr("x", function (d) {
        return (d.source.x + d.target.x) / 2 - sbolImgSize / 2;
      })
      .attr("y", function (d) {
        return (d.source.y + d.target.y) / 2 - sbolImgSize / 2;
      });
    });
  }

  setHistory(graph){
    console.log(graph);

    let zoom = d3.behavior.zoom()
      .scaleExtent([1, 10])
      .on("zoom", () => {
        svg.attr("transform", "translate(" + d3.event.translate + ")scale(" + d3.event.scale + ")");
      });

    let force = (this.layout = d3.layout.force());
    let drag = force.drag()
      .on("dragstart", function (d) {
        d3.event.sourceEvent.stopPropagation();
      });
    force.charge(-400).linkDistance(100);

    let width = $(this.id).parent().width();
    let height = $(this.id).parent().height();
    force.nodes(graph.nodes).links(graph.links).size([width, height]).start();

    let svg = d3.select(this.id).call(zoom).append("svg:g");
    svg.append('defs').append('marker')
      .attr('id', 'endArrow')
      .attr('viewBox', '0 -5 10 10')
      .attr('refX', 6)
      .attr('markerWidth', 6)
      .attr('markerHeight', 6)
      .attr('orient', 'auto')
      .append('path')
      .attr('d', 'M0,-5L10,0L0,5')
      .attr('fill', '#000');

    force.nodes(graph.nodes).links(graph.links).start();

    var link = svg.selectAll(".link")
      .data(graph.links).enter()
      .append("path").attr("class", "link");

    var node = svg.selectAll(".node")
      .data(graph.nodes).enter()
      .append("rect")
      .attr("class", function (d) {
        return "node " + d.knoxClass;
      }) // this changes the border & font of the node, but idk from where
      .attr("width", 60)
      .attr("height", 20)
      .style("fill", function(d){
        if (d.knoxClass === knoxClass.HEAD){
          return "#f96a17";
        }
        if (d.knoxClass === knoxClass.BRANCH){
          return "#0cc5b6";
        }
        if (d.knoxClass === knoxClass.COMMIT){
          return "#7e7e7e";
        }
      })
      .call(drag);

    var text = svg.selectAll("text.label")
      .data(graph.nodes).enter()
      .append("text")
      .attr("class", "label")
      .attr("text-anchor", "middle")
      .attr("fill", "black")
      .text(function(d) { return d.knoxID; });

    // force feed algo ticks
    force.on("tick", function() {

      link.attr('d', function(d) {
        var yPadding = 12,
          sourceX = d.source.x,
          sourceY = d.source.y + yPadding,
          targetX = d.target.x,
          targetY = d.target.y - yPadding;
        return 'M' + sourceX + ',' + sourceY + 'L' + targetX + ',' + targetY;
      });

      node.attr("x", function(d) { return d.x - 30; })
        .attr("y", function(d) { return d.y - 10 });

      text.attr("transform", function(d) {
        return "translate(" + d.x + "," + (d.y + 3) + ")";
      });

    });
  }

}