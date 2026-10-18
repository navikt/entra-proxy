document.querySelectorAll('[data-href]').forEach(knapp =>
    knapp.addEventListener('click', () => location.href = knapp.dataset.href));
