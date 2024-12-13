<script setup lang="ts">
import {useTeamsheetStore} from "@/utils/teamsheet_store";
import {type DatabaseTournament, tournamentByDateSortComparator} from "@/types/tournament_types";
import {DateTime} from "luxon";
import {computed, onMounted, ref} from "vue";
import {useRouter} from "vue-router";
import {loadTeamsheetPlayersFromFileKey, setTeamsheetMetaData} from "@/utils/api/teamsheet_api";
import type {Team} from "@/types/team_types";
import {GameCode} from "@/types";
import {
  newTeamsheetWithClubAndTournamentDataDEO,
  type TeamsheetUploadSuccessDEO,
  type TeamsheetWithClubAndTournamentDataDEO
} from "@/types/teamsheet_types";
import UploadTeamsheetComponent from "@/components/teamsheet/UploadTeamsheetComponent.vue";
import CommonEditTeamsheetData from "@/components/teamsheet/CommonEditTeamsheetData.vue";

const store = useTeamsheetStore()
const router = useRouter()
const props = defineProps<{
  fileKey: string
}>()

const futureTournaments = computed(() => {
  return store.publicStore.tournaments.filter(function (tournament: DatabaseTournament) {
    if (tournament.isLeague === true && tournament.endDate) {
      return tournament.endDate.endOf('day') > DateTime.now().startOf('day')
    } else {
      return tournament.date.endOf('day') > DateTime.now().startOf('day')
    }
  }).sort(tournamentByDateSortComparator)
})

const clubs = computed(() => {
  return store.publicStore.teams.filter((team) => team.isAmalgamation === false)
})



const teamsheetData = ref<TeamsheetWithClubAndTournamentDataDEO>(newTeamsheetWithClubAndTournamentDataDEO())

const isSending = ref<boolean>(false)
const isUploading = ref<boolean>(false)

onMounted(() => {

  if (!store.uploadSuccessDEO) {
    const fileKey = props.fileKey
    console.log("fileKey", fileKey)
    if (fileKey) {
      loadTeamsheetPlayersFromFileKey(fileKey)
          .then((response) => {
            console.log("response", response)
            store.uploadSuccessDEO = response
            loadStoreDataIntoTeamsheetData()
          })
          .catch((e) => {
            store.newError(e)
            router.push("/")
          })

    } else {
      loadStoreDataIntoTeamsheetData()
      //router.push("/")
    }

  }

})

function loadStoreDataIntoTeamsheetData() {
  teamsheetData.value.players = store.uploadSuccessDEO?.players || []
  teamsheetData.value.fileKey = store.uploadSuccessDEO?.fileKey || ""
}

function sendAugmentedData() {
  const players = teamsheetData.value.players
  if (players.length === 0) {
    store.newError("No players found in the teamsheet")
    return
  }
  if (teamsheetData.value.tournamentId == -1) {
    store.newError("Please select a tournament")
    return
  }
  if (teamsheetData.value.clubId == -1) {
    store.newError("Please select a team")
    return
  }
  if (teamsheetData.value.registrarName == "") {
    store.newError("Please enter your name")
    return
  }
  if (teamsheetData.value.registrarMail == "") {
    store.newError("Please enter your email")
    return
  }
  if (teamsheetData.value.codeId == -1) {
    store.newError("Please select a code")
    return
  }
  if (teamsheetData.value.fileKey == "") {
    store.newError("No file key found")
    return
  }
  isSending.value = true
  const metadata = {
    players: teamsheetData.value.players,
    clubId:teamsheetData.value.clubId,
    tournamentId: teamsheetData.value.tournamentId,
    registrarName: teamsheetData.value.registrarName,
    registrarMail: teamsheetData.value.registrarMail,
    fileKey: teamsheetData.value.fileKey,
    codeId: teamsheetData.value.codeId
  } as TeamsheetWithClubAndTournamentDataDEO
  setTeamsheetMetaData(metadata)
      .then(() => {
        isSending.value = false
        router.push({name: "teamsheet-complete", params: {fileKey: teamsheetData.value.fileKey}})
      })
      .catch((e) => {
        isSending.value = false
        store.newError(e)
      })

}

function onUploadComplete(newFileKey: string) {
  isUploading.value = false
  console.log("Upload complete")
  router.push({name: "augment-data", params: {fileKey: newFileKey}})
}
</script>

<template>
  <div>
    <template v-if="store.uploadSuccessDEO">
      <CommonEditTeamsheetData
          :model-value="teamsheetData"
          @onFileKeyChanged="onUploadComplete"
          @submitData="sendAugmentedData"
          />
    </template>
    <template v-else>
      <h3>Loading...</h3>
    </template>

  </div>
</template>

<style scoped>

</style>
