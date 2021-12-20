
export function capitalCase(stringValue: string): string {
    if (stringValue === "") {
        return ""
    } else {
        return stringValue[0].toUpperCase() + stringValue.slice(1).toLowerCase()
    }
}