(() => {
    const knapp = document.getElementById('oppfrisk-caches');
    const status = document.getElementById('cache-status');

    knapp.addEventListener('click', async () => {
        knapp.disabled = true;
        status.hidden = false;
        status.classList.remove('error');
        status.textContent = 'Oppfrisker cache...';

        try {
            const response = await fetch('/api/v1/cache/flush', {
                method: 'POST',
                headers: {'Accept': 'application/json'},
                redirect: 'error'
            });
            if (!response.ok) {
                let melding = `Kunne ikke oppfriske cache (HTTP ${response.status}).`;
                if (response.headers.get('content-type')?.includes('json')) {
                    const problem = await response.json();
                    melding = problem.detail ?? melding;
                }
                throw new Error(melding);
            }
            const antallSlettet = await response.json();
            status.textContent = `Cache oppfrisket. ${antallSlettet} nøkler slettet.`;
        } catch (error) {
            status.textContent = `Oppfrisking feilet: ${error.message}`;
            status.classList.add('error');
        } finally {
            knapp.disabled = false;
        }
    });
})();
