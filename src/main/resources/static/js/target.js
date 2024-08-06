import {knoxClass, getSBOLImage, splitElementID, condenseVisualization} from "./knox.js";

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
    console.log(graph);
    condenseVisualization(graph);

    var zoom = d3.behavior.zoom()
      .scaleExtent([1, 10])
      .on("zoom", () => {
        svg.attr("transform", "translate(" + d3.event.translate + ")scale(" + d3.event.scale + ")");
      });

    //add SVG container
    let svg = d3.select(this.id)
      .call(zoom)
      .append("svg:g");

    //def objects are not displayed until referenced
    let defs = svg.append("svg:defs");

    let force = (this.layout = d3.layout.force())
      .charge(-400)
      .linkDistance(100)
      .nodes(graph.nodes)
      .links(graph.links)
      .size([$(this.id).parent().width(), $(this.id).parent().height()])
      .start();
    force.drag().on("dragstart", () => {
      d3.event.sourceEvent.stopPropagation();
    });

    // add nodes (circles)
    let nodesEnter = svg.selectAll(".node")
      .data(graph.nodes)
      .enter();

    let circles = nodesEnter.append("circle")
      .attr("class", function(d) {
        if (d.nodeTypes.length === 0) {
          return "node";
        } else if (d.nodeTypes.indexOf("start") >= 0) {
          return "start-node";
        } else if (d.nodeTypes.indexOf("accept") >= 0) {
          return "accept-node";
        }
      })
      .attr("r", 7) //radius
      .call(force.drag);

    // Filter out links if the "show" flag is false
    let linksEnter = svg.selectAll(".link")
      .data(graph.links.filter(link => link.show))
      .enter();

    function marker(isBlank) {

      let fill = isBlank? "none": "#999";
      let id = "arrow"+fill.replace("#", "");

      defs.append("svg:marker")
        .attr("id", id)
        .attr("viewBox", "0 -5 10 10")
        .attr("refX", 6)
        .attr("markerWidth", 6)
        .attr("markerHeight", 6)
        .attr("orient", "auto")
        .append("svg:path")
        .attr("d", "M0,-5L10,0L0,5")
        .attr("stroke", "#999")
        .attr("fill", fill);

      return "url(#" + id + ")";
    }

    // Optional links will be rendered as dashed lines
    // Blank edges will be rendered with an unfilled arrow
    let links = linksEnter.append("path")
      .attr("class", (l) => {
        if (l.optional){
          return "link dashed-link";
        }
        return "link"
      })
      .attr("d", "M0,-5L10,0L0,5")
      .attr("marker-end",(l) => {
        let isBlank = l["componentRoles"].length === 0 && l["componentIDs"].length === 0;
        return marker(isBlank);
      });

    //place SBOL svg on links
    const sbolImgSize = 30;
    let images = linksEnter.append("svg:image")
      .attr("height", sbolImgSize)
      .attr("width", sbolImgSize)
      .attr("class", (d) => {
        if ((d.hasOwnProperty("componentRoles") && d["componentRoles"].length > 0) || (d.hasOwnProperty("weight") && d["weight"].length > 0)) {
          return "sboltip";
        }
        return null;
      })
      .attr("title", (d) => {
        if (d.hasOwnProperty("componentIDs") && d["componentIDs"].length > 0) {
          let titleStr = "";
          const length = d["componentIDs"].length;
          for(let i=0; i<d["componentIDs"].length; i++){
            titleStr += splitElementID(d["componentIDs"][i]);
            if (length !== i+1){
              titleStr += ",";
            }
          }

          if (d.hasOwnProperty("weight")) {
            titleStr += ",";
            const lengthWeights = d["weight"].length;
            for(let i=0; i<d["weight"].length; i++){
              titleStr += d["weight"][i];
              if (lengthWeights !== i+1){
                titleStr += ",";
              }
            }
          } 

          return titleStr;
        } else if (d.hasOwnProperty("weight")) {
          let titleStr = "";
          const lengthWeights = d["weight"].length;
          for(let i=0; i<d["weight"].length; i++){
            titleStr += d["weight"][i];
            if (lengthWeights !== i+1){
              titleStr += ",";
            }
          }

          return titleStr
        }
      })
      .attr("href", (d) => {
        if (d.hasOwnProperty("componentRoles") && d["componentRoles"].length > 0) {
          return getSBOLImage(d["componentRoles"][0]);
        }
        return null;
      });

    let reverseImgs = linksEnter.append("svg:image")
      .attr("height", sbolImgSize)
      .attr("width", sbolImgSize)
      .attr("href", (d) => {
        if (d.hasOwnProperty("componentRoles")) {
          if (d["componentRoles"].length > 0 && d.hasReverseOrient) {
            return getSBOLImage(d["componentRoles"][0]);
          }
        }
        return null;
      });

    //place tooltip on the SVG images
    $('.sboltip').tooltipster({
      theme: 'tooltipster-shadow',
      trigger: 'custom',
      triggerOpen: {
      mouseenter: true
      },
      triggerClose: {
      click: true,
      }
    });

    // Handles positioning when moved
    force.on("tick", function () {

      // Position links
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

      // Position circles
      circles.attr("cx", function (d) {
        return d.x;
      })
      .attr("cy", function (d) {
        return d.y;
      });

      // Position SBOL images
      images.attr("x", function (d) {
          if(d.hasReverseOrient){
            return (d.source.x + d.target.x) / 2 - sbolImgSize;
          }
          return (d.source.x + d.target.x) / 2 - sbolImgSize / 2;
        })
        .attr("y", function (d) {
          return (d.source.y + d.target.y) / 2 - sbolImgSize / 2;
        })
        .attr('transform',function(d){
          //transform 180 if the orientation is REVERSE_COMPLEMENT
          if(d.orientation === "REVERSE_COMPLEMENT" && !d.hasReverseOrient){
            let x1 = (d.source.x + d.target.x) / 2; //the center x about which you want to rotate
            let y1 = (d.source.y + d.target.y) / 2; //the center y about which you want to rotate
            return `rotate(180, ${x1}, ${y1})`;
          }
        });

      reverseImgs.attr("x", function (d) {
        return (d.source.x + d.target.x) / 2 - sbolImgSize;
      })
      .attr("y", function (d) {
        return (d.source.y + d.target.y) / 2 - sbolImgSize / 2;
      })
      .attr('transform',function(d){
        let x1 = (d.source.x + d.target.x) / 2; //the center x about which you want to rotate
        let y1 = (d.source.y + d.target.y) / 2; //the center y about which you want to rotate
        return `rotate(180, ${x1}, ${y1})`;
      });
    });
  }

  setHistory(graph){
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
