<script setup lang="ts">

import {type DatabaseTournament, tournamentByDateSortComparator} from "@/types/tournament_types";
import UploadTeamsheetComponent from "@/components/teamsheet/UploadTeamsheetComponent.vue";
import {
  type PlayerDEO,
  type TeamsheetUploadSuccessDEO,
  TeamsheetWithClubAndTournamentDataDEO
} from "@/types/teamsheet_types";
import {computed, ref} from "vue";
import {DateTime} from "luxon";
import {useTeamsheetStore} from "@/utils/teamsheet_store";

const model = defineModel<TeamsheetWithClubAndTournamentDataDEO>({
  required: true
})


const emits = defineEmits<{
  (e: 'submitData'): void
  (e: 'onFileKeyChanged', newFileKey:string): void
}>()

function sendAugmentedData() {
  emits('submitData')
}

function onUploadComplete(response: TeamsheetUploadSuccessDEO) {
  model.value.players = response.players
  emits('onFileKeyChanged', response.fileKey)
}
const store = useTeamsheetStore()

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

const isSending = ref<boolean>(false)
const isUploading = ref<boolean>(false)

function findTournamentById(id: number): DatabaseTournament | undefined {
  return store.publicStore.tournaments.find((tournament) => tournament.id === id)
}
let tournament: DatabaseTournament | undefined
</script>

<template>
  <h3>Please complete your Teamsheet data</h3>
  <p>Tournament:</p>
  <Dropdown
      v-model="model.tournamentId"
      :options="futureTournaments"
      placeholder="Select a Tournament"
      optionValue="id"
      :disabled="isSending"
  >
    <template #option="{option,index} : {option:DatabaseTournament, index:number}">
      <div >
        {{ option.date.toISODate() }} - {{ option.name }} - {{ option.location }}
      </div>
    </template>
    <template #value="{value,placeholder}:{value:number,placeholder:string}">
      <div v-if="value != -1">
        {{ void(tournament = findTournamentById(value)) }}
        {{ tournament?.date.toISODate() }} - {{ tournament?.name }} - {{ tournament?.location }}
      </div>
      <div v-else>{{ placeholder }}</div>
    </template>
  </Dropdown>
  <p>Code:</p>
  <Dropdown
      v-model="model.codeId"
      :options="store.publicStore.codes"
      optionLabel="name"
      optionValue="id"
      placeholder="Select a Code"
      :disabled="isSending"
  ></Dropdown>

  <p>Your Club</p>
  <Dropdown
      v-model="model.clubId"
      :options="clubs"
      option-label="name"
      option-value="id"
      filter
      placeholder="Select your club"
      :disabled="isSending"
  ></Dropdown>

  <p>Your Name</p>
  <InputText v-model="model.registrarName" placeholder="Enter your name" :disabled="isSending"></InputText>
  <p>Your Email</p>
  <InputText
      v-model="model.registrarMail"
      placeholder="Enter your mail (@gaa.ie)"
      :pt="{root:{class: 'w-96'}}"
      :disabled="isSending"
  ></InputText>

  <p>Players: {{ model.players.length }}</p>
  <table>
    <tr>
      <th>Player Number</th>
      <th>Name</th>
      <th>Jersey Number</th>
    </tr>
    <tr v-for="player in model.players">
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

<style scoped>

</style>
