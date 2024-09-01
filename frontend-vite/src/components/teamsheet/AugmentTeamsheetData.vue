<script setup lang="ts">
import {useTeamsheetStore} from "@/utils/teamsheet_store";
import {type DatabaseTournament, tournamentByDateSortComparator} from "@/types/tournament_types";
import {DateTime} from "luxon";
import {computed, onMounted, ref} from "vue";
import {useRouter} from "vue-router";
import {loadTeamsheetPlayersFromFileKey, setTeamsheetMetaData} from "@/utils/api/teamsheet_api";
import type {Team} from "@/types/team_types";
import {GameCode} from "@/types";
import type {TeamsheetUploadSuccessDEO, TeamsheetWithClubAndTournamentDataDEO} from "@/types/teamsheet_types";
import UploadTeamsheetComponent from "@/components/teamsheet/UploadTeamsheetComponent.vue";

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

const selectedTournament = ref<DatabaseTournament | null>(null)
const selectedTeam = ref<Team | null>(null)
const registrarName = ref<string>("")
const registrarEmail = ref<string>("")
const selectedCode = ref<GameCode>()

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
          })
          .catch((e) => {
            store.newError(e)
            router.push("/")
          })

    } else {
      //router.push("/")
    }

  }
})

function sendAugmentedData() {
  const players = store.uploadSuccessDEO?.players || []
  if (players.length === 0) {
    store.newError("No players found in the teamsheet")
    return
  }
  if (!selectedTournament.value) {
    store.newError("Please select a tournament")
    return
  }
  if (!selectedTeam.value) {
    store.newError("Please select a team")
    return
  }
  if (!registrarName.value) {
    store.newError("Please enter your name")
    return
  }
  if (!registrarEmail.value) {
    store.newError("Please enter your email")
    return
  }
  if (!selectedCode.value) {
    store.newError("Please select a code")
    return
  }
  if (!store.uploadSuccessDEO?.fileKey) {
    store.newError("No file key found")
    return
  }
  isSending.value = true
  const metadata = {
    players: store.uploadSuccessDEO?.players,
    clubId: selectedTeam.value?.id,
    tournamentId: selectedTournament.value?.id,
    registrarName: registrarName.value,
    registrarMail: registrarEmail.value,
    fileKey: store.uploadSuccessDEO?.fileKey,
    codeId: selectedCode.value?.id
  } as TeamsheetWithClubAndTournamentDataDEO
  setTeamsheetMetaData(metadata)
      .then(() => {
        isSending.value = false
        router.push({name: "teamsheet-complete", params: {fileKey: store.uploadSuccessDEO?.fileKey}})
      })
      .catch((e) => {
        isSending.value = false
        store.newError(e)
      })

}

function onUploadComplete(response: TeamsheetUploadSuccessDEO) {
  isUploading.value = false
  console.log("Upload complete")
  router.push({name: "augment-data", params: {fileKey: response.fileKey}})
}
</script>

<template>
  <div>
    <template v-if="store.uploadSuccessDEO">
      <h3>Please complete your Teamsheet data</h3>
      <p>Tournament:</p>
      <Dropdown
          v-model="selectedTournament"
          :options="futureTournaments"
          optionLabel="name"
          placeholder="Select a Tournament"
          :disabled="isSending"
      >
        <template #option="{option,index} : {option:DatabaseTournament, index:number}">
          <div>{{ option.date.toISODate() }} - {{ option.name }} - {{ option.location }}</div>
        </template>
        <template #value="{value,placeholder}:{value:DatabaseTournament,placeholder:string}">
          <div v-if="value">{{ value.date.toISODate() }} - {{ value.name }} - {{ value.location }}</div>
          <div v-else>{{ placeholder }}</div>
        </template>
      </Dropdown>
      <p>Code:</p>
      <Dropdown
          v-model="selectedCode"
          :options="store.publicStore.codes"
          optionLabel="name"
          placeholder="Select a Code"
          :disabled="isSending"
      ></Dropdown>

      <p>Your Club</p>
      <Dropdown
          v-model="selectedTeam"
          :options="clubs"
          option-label="name"
          filter
          placeholder="Select your club"
          :disabled="isSending"
      ></Dropdown>

      <p>Your Name</p>
      <InputText v-model="registrarName" placeholder="Enter your name" :disabled="isSending"></InputText>
      <p>Your Email</p>
      <InputText
          v-model="registrarEmail"
          placeholder="Enter your mail (@gaa.ie)"
          :pt="{root:{class: 'w-96'}}"
          :disabled="isSending"
      ></InputText>

      <p>Players: {{ store.uploadSuccessDEO?.players?.length }}</p>
      <table>
        <tr>
          <th>Player Number</th>
          <th>Name</th>
          <th>Jersey Number</th>
        </tr>
        <tr v-for="player in store.uploadSuccessDEO?.players">
          <td class="m-2">{{ player.playerNumber }}</td>
          <td class="m-2">{{ player.name }}</td>
          <td class="m-2">
            <InputNumber
                v-model="player.jerseyNumber"
                placeholder="Jersey number"
                :max="200"
                :max-fraction-digits="0"
                :allow-empty="true"
                input-class="w-32"
                :disabled="isSending"
            />
          </td>
        </tr>
      </table>

      <p>Note: You can enter the jersey numbers at a later point before the tournament or on the day of the tournament
        itself.</p>
      <Accordion
          :active-index="-1"
          :pt="{'root':{class:'m-4'}}"
      >
        <AccordionTab header="Upload updated teamsheet">

          <UploadTeamsheetComponent
              @upload-complete="onUploadComplete"
              @upload-start="isUploading = true"
              @upload-failed="isUploading = false"

          />
        </AccordionTab>
      </Accordion>
      <Button :disabled="isUploading || isSending" @click="sendAugmentedData()">Submit</Button>
    </template>
    <template v-else>
      <h3>Loading...</h3>
    </template>

  </div>
</template>

<style scoped>

</style>
