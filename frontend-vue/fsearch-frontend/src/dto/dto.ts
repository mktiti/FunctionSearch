
interface FunDocDto {
    shortInfo: string | null;
    details: string | null;
}

interface QueryFitResult {
    file: string;
    funName: string;
    static: boolean;
    header: string;
    doc: FunDocDto;
}

type InternalErrorResult = {
    type: 'QueryResult$Error$Internal';

}
