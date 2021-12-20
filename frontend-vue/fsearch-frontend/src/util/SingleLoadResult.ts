export type SingleLoadResult<R> = R | "Loading" | "Error"

export function isNotLoaded(res: SingleLoadResult<any>): boolean {
    return res === "Loading" || res === "Error"
}

export function isLoaded(res: SingleLoadResult<any>): boolean {
    return !isNotLoaded(res)
}
