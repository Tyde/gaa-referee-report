<script setup lang="ts">

import {computed, onMounted, ref} from "vue";
import {DateTime} from "luxon";
import {DatabaseTournament, Tournament} from "@/types/tournament_types";
import {useReportStore} from "@/utils/edit_report_store";
import {uploadNewTournament} from "@/utils/api/tournament_api";
import {useI18n} from "vue-i18n";

const props = defineProps<{
  preselectedDate: Date,
}>()

const store = useReportStore()

const emit = defineEmits<{
  (e: 'tournament_created', tournament: DatabaseTournament): void,
  (e: 'canceled') :void
}>()
const editedTournament = ref<Tournament>({
  date: DateTime.now(), location: "", name: "", region: 0, isLeague: false, endDate: null
})
const allowChangeDate = ref<Boolean>(false)
const isLoading = ref<Boolean>(true)
onMounted(() => {
  editedTournament.value.date = DateTime.fromJSDate( props.preselectedDate)
})
const {t} = useI18n()

async function save_tournament() {
  if (editedTournament.value.location && editedTournament.value.name && editedTournament.value.region) {

    if(editedTournament.value.isLeague && !editedTournament.value.endDate) {
      store.newError(t('tournament.error.noEndDate'))
      return
    }
    isLoading.value = true


    uploadNewTournament(editedTournament.value)
        .then((newTournament) => emit('tournament_created',newTournament))
        .catch((e) => {
          store.newError(e)
        })
        .finally(() => {
          isLoading.value = false
        })


  } else {
    store.newError(t('tournament.error.missingFields'))
  }
}

const dateString = computed(() => {
  return editedTournament.value.date.toISODate()
})
</script>

<template>
  <Card>
    <template #title>
      {{ $t('tournament.create') }}
    </template>
    <template #content>
      <div class="flex align-items-center">
        <Checkbox binary v-model="editedTournament.isLeague" id="isLeague" />
        <label for="isLeague" class="ml-2">{{ $t('tournament.isLeague') }}</label>
        <!-- info icon with tooltip -->

        <i v-tooltip:top="$t('tournament.leagueInfo')" class="pi pi-info-circle ml-2 mt-1"></i>
      </div>
      <div class="flex flex-row" v-if="allowChangeDate || editedTournament.isLeague">
        <div class="flex flex-col align-center m-2">
          <label v-if="!editedTournament.isLeague" for="dateformat">{{ $t('tournament.dateinput.tournament') }}</label>
          <label v-else for="dateformat">{{ $t('tournament.dateinput.startLeague') }}</label>

          <Calendar id="dateformat"
                  :model-value="editedTournament.date.toJSDate()"
                  @update:model-value="(newDate:Date) => {editedTournament.date = DateTime.fromJSDate(newDate)}"
                  dateFormat="yy-mm-dd"/>
        </div>
        <div v-if="editedTournament.isLeague" class="flex flex-col align-center m-2">
          <label for="dateend">{{ $t('tournament.dateinput.endLeague') }}</label>
          <Calendar id="dateend"
                  :model-value="editedTournament.endDate?.toJSDate()"
                  @update:model-value="(newDate:Date) => {editedTournament.endDate = DateTime.fromJSDate(newDate)}"
                  dateFormat="yy-mm-dd"
                  />
        </div>
      </div>
      <template v-else> <h3>Date: {{ dateString }}</h3>
        <Button class="p-button-secondary" @click="allowChangeDate = true">{{ $t('tournament.changeDate') }}</Button>
      </template>
      <br><br>
      <span class="p-float-label">
        <InputText v-model="editedTournament.name" id="name"/>
        <label for="name">{{ $t('tournament.enterName') }}</label>
      </span><br>
      <span class="p-float-label">
        <InputText v-model="editedTournament.location" id="location"/>
        <label for="location">{{ $t('tournament.enterLocation') }}</label>
      </span><br>

        <Dropdown
            v-model="editedTournament.region"
            id="region"
            :options="store.publicStore.regions"
            option-label="name"
            option-value="id"
            :placeholder="$t('tournament.selectRegion')"
            />
      <div class="flex flex-row">
        <div class="m-2">
          <Button @click="save_tournament">{{ $t('general.submit') }}</Button>
        </div>
        <div class="m-2">
          <Button @click="emit('canceled')" class="p-button-danger">{{ $t('general.cancel') }}</Button>
        </div>
      </div>
    </template>
  </Card>
</template>

<style scoped>
h3 {
  @apply m-2;
  @apply text-xl font-bold;
}
</style>
