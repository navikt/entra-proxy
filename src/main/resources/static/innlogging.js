let loggerInnPaNytt = false;

async function hentMedInnlogging(url, innstillinger = {}) {
    const response = await fetch(url, innstillinger);

    if (response.status === 401) {
        if (!loggerInnPaNytt) {
            loggerInnPaNytt = true;
            window.location.reload();
        }
        throw new Error('Innloggingen er utl\u00f8pt. Logger inn p\u00e5 nytt...');
    }

    return response;
}
