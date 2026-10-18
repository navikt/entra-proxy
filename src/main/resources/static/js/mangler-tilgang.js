    (function () {
        const navident = new URLSearchParams(window.location.search).get("navident");
        if (navident) {
            document.getElementById("navident-verdi").textContent = navident;
            document.getElementById("navident").hidden = false;
        }
    })();
