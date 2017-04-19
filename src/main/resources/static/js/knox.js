(function() {
    // left: 37, up: 38, right: 39, down: 40,
    // spacebar: 32, pageup: 33, pagedown: 34, end: 35, home: 36
    var keys = {37: 1, 38: 1, 39: 1, 40: 1};

    function preventDefault(e) {
        e = e || window.event;
        if (e.preventDefault)
            e.preventDefault();
        e.returnValue = false;  
    }

    function preventDefaultForScrollKeys(e) {
        if (keys[e.keyCode]) {
            preventDefault(e);
            return false;
        }
    }

    function disableScroll() {
        if (window.addEventListener) // older FF
            window.addEventListener("DOMMouseScroll", preventDefault, false);
        window.onwheel = preventDefault; // modern standard
        window.onmousewheel = document.onmousewheel = preventDefault; // older browsers, IE
        window.ontouchmove  = preventDefault; // mobile
        document.onkeydown  = preventDefaultForScrollKeys;
    }

    window.disableScroll = disableScroll;
})();

window.onload = function() {
    disableScroll();
};

(function($) {
    "use strict";

    $(document).on("click", "a.page-scroll", function(event) {
        var $anchor = $(this);
        $("html, body").stop().animate({
            scrollTop: ($($anchor.attr("href")).offset().top - 50)
        }, 1250, "easeInOutExpo");
        event.preventDefault();
    });

    $("body").scrollspy({
        target: ".navbar-fixed-top",
        offset: 51
    });

    $(".navbar-collapse ul li a").click(function() {
        $(".navbar-toggle:visible").click();
    });

    $("#mainNav").affix({
        offset: {
            top: 100
        }
    });
    
    window.sr = ScrollReveal();
    sr.reveal(".sr-icons", {
        duration: 600,
        scale: 0.3,
        distance: "0px"
    }, 200);
    sr.reveal(".sr-button", {
        duration: 1000,
        delay: 200
    });
    sr.reveal(".sr-contact", {
        duration: 600,
        scale: 0.3,
        distance: "0px"
    }, 300);
})(jQuery);
