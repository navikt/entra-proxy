interface Problem {
    detail?: string | null;
}

export async function feilFraRespons(response: Response, standardmelding: string): Promise<Error> {
    if (response.headers.get('content-type')?.includes('json')) {
        const problem: Problem = await response.json();
        return new Error(problem.detail ?? standardmelding);
    }
    return new Error(standardmelding);
}

export async function hentJson<T>(url: string, feilprefiks: string): Promise<T> {
    const response = await fetch(url, {headers: {'Accept': 'application/json'}});
    if (!response.ok) {
        throw await feilFraRespons(response, `${feilprefiks} (HTTP ${response.status}).`);
    }
    return await response.json();
}

export async function hentTekst(url: string, feilprefiks: string): Promise<string> {
    const response = await fetch(url, {headers: {'Accept': 'text/plain'}});
    if (!response.ok) {
        throw new Error(`${feilprefiks} (HTTP ${response.status}).`);
    }
    return (await response.text()).trim();
}
