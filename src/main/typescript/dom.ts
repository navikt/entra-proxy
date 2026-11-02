export function hentElement<T extends Element>(
    selector: string,
    type: {new(): T},
    root: ParentNode = document
): T {
    const element = root.querySelector(selector);
    if (!(element instanceof type)) {
        throw new Error(`Mangler forventet element: ${selector}`);
    }
    return element;
}

export function feilmelding(error: unknown): string {
    return error instanceof Error ? error.message : String(error);
}
