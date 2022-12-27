import {defineStore} from "pinia";
import {ref} from "vue";
import type {PitchVariables} from "@/utils/api/pitch_api";
import {getPitchVariables, updatePitchPropertyOnServer} from "@/utils/api/pitch_api";
import type {PitchProperty} from "@/types";
import {PitchPropertyType} from "@/types";

export const useAdminStore = defineStore('admin', () => {
    const pitchVariables = ref<PitchVariables>()
    const currentError = ref<string | undefined>()

    function fetchPitchVariables() {
        getPitchVariables()
            .then(it => pitchVariables.value = it)
            .catch(
                (error) => {
                    currentError.value = error
                }
            )
    }

    function getVariablesByType(type: PitchPropertyType) {
        switch (type) {
            case PitchPropertyType.goalDimensions:
                return pitchVariables.value?.goalDimensions || []
            case PitchPropertyType.goalPosts:
                return pitchVariables.value?.goalPosts || []
            case PitchPropertyType.length:
                return pitchVariables.value?.lengths || []
            case PitchPropertyType.width:
                return pitchVariables.value?.widths || []
            case PitchPropertyType.markingsOptions:
                return pitchVariables.value?.markingsOptions || []
            case PitchPropertyType.surface:
                return pitchVariables.value?.surfaces || []
        }
        return []
    }

    async function updatePitchVariable(option: PitchProperty, type: PitchPropertyType) {
        let prop = getVariablesByType(type).find(it => it.id === option.id)
        if (prop) {
            prop.name = option.name
            await updatePitchPropertyOnServer(prop)
        }
    }

    return {
        pitchVariables,
        fetchPitchVariables,
        getVariablesByType,
        updatePitchVariable,
        currentError
    }
})