<script setup lang="ts">
import {useTeamsheetStore} from "@/utils/teamsheet_store";
import {onBeforeMount, ref} from "vue";
import {getTeamsheetMetaData} from "@/utils/api/teamsheet_api";
import {useRouter} from "vue-router";
import {newTeamsheetWithClubAndTournamentDataDEO, TeamsheetWithClubAndTournamentDataDEO} from "@/types/teamsheet_types";
import AugmentTeamsheetData from "@/components/teamsheet/AugmentTeamsheetData.vue";
import CommonEditTeamsheetData from "@/components/teamsheet/CommonEditTeamsheetData.vue";

const store = useTeamsheetStore();
const router = useRouter()
const props = defineProps<{
  fileKey: string
}>()

const metaData = ref<TeamsheetWithClubAndTournamentDataDEO>(newTeamsheetWithClubAndTournamentDataDEO())
onBeforeMount(() => {
  getTeamsheetMetaData(props.fileKey)
      .then((response) => {
        metaData.value = response
      })
      .catch((e) => {
        store.newError(e)
        //router.push("/")
      })
});

function onFileKeyChanged(newFileKey: string) {

}

function submitEdit() {

}
</script>

<template>
<CommonEditTeamsheetData :model-value="metaData" @submitData="submitEdit" @onFileKeyChanged="onFileKeyChanged"/>
</template>

<style scoped>

</style>
