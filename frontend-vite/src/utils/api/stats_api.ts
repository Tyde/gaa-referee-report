import { parseAndHandleDEO } from "@/utils/api/api_utils";
import { StatsDEO } from "@/types/stats_types";

export async function loadStats(): Promise<StatsDEO> {
    return fetch("/api/stats")
        .then(response => response.json())
        .then(data => parseAndHandleDEO(data, StatsDEO));
}
