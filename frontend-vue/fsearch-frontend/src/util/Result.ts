import {QueryResult} from "@/service/generated-client";

type ResType = {
    type: string;
}

export function isSuccess(result: QueryResult | null): boolean {
    return (result !== null) && (result as object as ResType).type == "QueryResult$Success";
}

export function isInternalError(result: QueryResult | null): boolean {
    return (result !== null) && (result as object as ResType).type == "QueryResult$Error$Internal";
}

export function isQueryError(result: QueryResult | null): boolean {
    return (result !== null) && (result as object as ResType).type == "QueryResult$Error$Query";
}

export function isError(result: QueryResult | null): boolean {
    return isInternalError(result) || isQueryError(result);
}
