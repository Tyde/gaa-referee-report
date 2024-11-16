<script setup lang="ts">
import {useTeamsheetStore} from "@/utils/teamsheet_store";
import {onBeforeMount, ref} from "vue";
import {getTeamsheetMetaData} from "@/utils/api/teamsheet_api";
import {useRouter} from "vue-router";
import {TeamsheetWithClubAndTournamentDataDEO} from "@/types/teamsheet_types";

const store = useTeamsheetStore();
const router = useRouter()
const props = defineProps<{
  fileKey: string
}>()

const metaData = ref<TeamsheetWithClubAndTournamentDataDEO>()
onBeforeMount(() => {
  getTeamsheetMetaData(props.fileKey)
      .then((response) => {
        metaData.value = response
      })
      .catch((e) => {
        store.newError(e)
        router.push("/")
      })
});
</script>

<template>
<div>
  Editing teamsheet
  {{props.fileKey}}

  sender:
  {{metaData?.players}}
</div>
</template>

<style scoped>

</style>
