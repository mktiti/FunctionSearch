import {LoginResult} from "fsearch_client";
import {ResType} from "@/util/Result";

export function isLoginSuccess(result: LoginResult | null): boolean {
    return (result !== null) && (result as object as ResType).type == "LoginResult$Success";
}

export function isLoginInvalid(result: LoginResult | null): boolean {
    return (result !== null) && (result as object as ResType).type == "LoginResult$Invalid";
}
