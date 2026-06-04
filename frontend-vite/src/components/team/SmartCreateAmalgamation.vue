<script lang="ts" setup>
import {computed, onBeforeMount, ref} from "vue";
import TeamSelectField from "@/components/team/TeamSelectField.vue";
import {createAmalgamationOnServer} from "@/utils/api/teams_api";
import {useReportStore} from "@/utils/edit_report_store";
import type {Team} from "@/types/team_types";

const store = useReportStore();
const props = defineProps<{
  rough_amalgamation_name?: string
}>()

const emit = defineEmits<{
  (e: 'on_new_amalgamation', team: Team): void,
  (e: 'on_cancel'): void
}>()

const step = ref<'name' | 'suggest' | 'manual'>('name')
const amalgamation_name = ref(props.rough_amalgamation_name || "")
const selected_teams = ref(<Team[]>[])
const is_loading = ref(false)

onBeforeMount(() => {
  store.publicStore.loadTeams()
})

const nameParts = computed(() => {
  const name = amalgamation_name.value.trim()
  if (!name) return []
  return name.split(/\s*[/&]\s*|\s+and\s+/i).map(s => s.trim()).filter(s => s.length > 0)
})

function normalizeForMatch(s: string): string {
  return s.toLowerCase().replace(/\./g, '')
}

function matchTeamName(searchPart: string, teamName: string): 'exact' | 'startsWith' | 'contains' | null {
  const normalizedSearch = normalizeForMatch(searchPart)
  const normalizedTeam = normalizeForMatch(teamName)
  
  const searchWords = normalizedSearch.split(/\s+/).filter(w => w.length > 0)
  const teamWords = normalizedTeam.split(/\s+/).filter(w => w.length > 0)
  
  const allWordsMatch = searchWords.every(sw => 
    teamWords.some(tw => tw === sw || tw.startsWith(sw) || tw.includes(sw))
  )
  
  if (!allWordsMatch) return null
  
  if (normalizedTeam === normalizedSearch) return 'exact'
  if (normalizedTeam.startsWith(normalizedSearch)) return 'startsWith'
  if (normalizedTeam.includes(normalizedSearch)) return 'contains'
  
  const allWordsExact = searchWords.every(sw =>
    teamWords.some(tw => tw === sw || tw.startsWith(sw))
  )
  if (allWordsExact) return 'startsWith'
  
  return 'contains'
}

const suggestedTeams = computed(() => {
  if (nameParts.value.length === 0) return []
  
  const teams = store.publicStore.teams.filter(team => {
    if (team.isAmalgamation && team.amalgamationTeams && team.amalgamationTeams.length > 1) {
      return false
    }
    return true
  })
  
  return nameParts.value.map(part => {
    const matches: { team: Team, match: 'exact' | 'startsWith' | 'contains' }[] = []
    
    teams.forEach(t => {
      const matchResult = matchTeamName(part, t.name)
      if (matchResult) {
        matches.push({ team: t, match: matchResult })
      }
    })
    
    matches.sort((a, b) => {
      const order = { exact: 0, startsWith: 1, contains: 2 }
      return order[a.match] - order[b.match]
    })
    
    return { part, matches }
  })
})

function toggleTeam(team: Team) {
  const idx = selected_teams.value.findIndex(t => t.id === team.id)
  if (idx >= 0) {
    selected_teams.value.splice(idx, 1)
  } else {
    selected_teams.value.push(team)
  }
}

function goToSuggestStep() {
  if (!amalgamation_name.value.trim()) {
    alert("Please enter an amalgamation name")
    return
  }
  selected_teams.value = suggestedTeams.value
    .filter(s => s.matches.length > 0)
    .map(s => s.matches[0].team)
  step.value = 'suggest'
}

function goToManual() {
  step.value = 'manual'
}

function goToSuggestFromManual() {
  step.value = 'suggest'
}

const duplicates = computed(() => {
  return store.publicStore.teams.filter(team => {
    if(team.isAmalgamation && team.amalgamationTeams) {
      if(team.amalgamationTeams.length === selected_teams.value.length) {
        let allTeamsInAmalgamation = true
        for (let otherAmalgamationTeam of team.amalgamationTeams) {
          let contained = selected_teams.value.filter(it => it.id === otherAmalgamationTeam.id).length > 0
          allTeamsInAmalgamation = allTeamsInAmalgamation && contained
        }
        return allTeamsInAmalgamation
      }
    }
    return false
  })
})

async function create_amalgamation() {
  if (!amalgamation_name.value.trim()) {
    alert("Please enter an amalgamation name")
    return
  }
  if (selected_teams.value.length < 2) {
    alert("Amalgamations must consist of at least two teams")
    return
  }
  
  is_loading.value = true
  const team = await createAmalgamationOnServer(amalgamation_name.value, selected_teams.value)
    .catch((error) => {
      store.newError(error)
      return undefined
    })
  is_loading.value = false
  if (team) {
    emit('on_new_amalgamation', team)
  }
}

function cancel() {
  emit('on_cancel')
}

function on_team_selected(team: Team) {
  if (!selected_teams.value.some(t => t.id === team.id)) {
    selected_teams.value.push(team)
  }
}
</script>

<template>
  <div class="w-fill shadow-xl p-4 bg-surface-700 border-surface-500 rounded-xl">
    <h3 class="text-lg font-semibold mb-4">{{ $t('teamSelect.newAmalgamationTitle') }}</h3>
    
    <!-- Step 1: Enter name -->
    <div v-if="step === 'name'" class="space-y-4">
      <div>
        <label for="amalgamation_name" class="block mb-2">{{ $t('teamSelect.newAmalgamationName') }}</label>
        <InputText 
          id="amalgamation_name" 
          v-model="amalgamation_name" 
          class="w-full"
          placeholder="e.g., Darmstadt / Frankfurt"
          @keyup.enter="goToSuggestStep"
        />
        <p class="text-sm text-gray-400 mt-2">Use "/" to separate team names for smart suggestions</p>
      </div>
      <div class="flex gap-2 justify-end">
        <Button class="p-button-secondary p-button-rounded" @click="cancel">{{ $t('general.cancel') }}</Button>
        <Button class="p-button-rounded" @click="goToSuggestStep">Next</Button>
      </div>
    </div>
    
    <!-- Step 2: Suggestions -->
    <div v-else-if="step === 'suggest'" class="space-y-4">
      <div class="bg-surface-600 p-3 rounded-lg">
        <span class="text-gray-400">Amalgamation:</span>
        <span class="font-semibold ml-2">{{ amalgamation_name }}</span>
      </div>
      
      <div v-if="suggestedTeams.length > 0">
        <h4 class="mb-2 font-medium">Suggested teams:</h4>
        <div class="space-y-3">
          <div 
            v-for="s in suggestedTeams" 
            :key="s.part"
            class="p-3 rounded-lg"
            :class="s.matches.length > 0 ? 'bg-surface-600' : 'bg-surface-800'"
          >
            <div class="font-medium mb-2">{{ s.part }}</div>
            <div v-if="s.matches.length === 0" class="text-sm text-red-400">
              No match found
            </div>
            <div v-else-if="s.matches.length === 1" class="flex items-center justify-between">
              <span class="text-sm text-gray-300">
                {{ s.matches[0].team.name }}
                <span v-if="s.matches[0].match !== 'exact'" class="text-yellow-400">({{ s.matches[0].match }} match)</span>
              </span>
              <Checkbox 
                :model-value="selected_teams.some(t => t.id === s.matches[0].team.id)"
                @update:model-value="toggleTeam(s.matches[0].team)"
                binary
              />
            </div>
            <div v-else class="space-y-1">
              <div 
                v-for="m in s.matches" 
                :key="m.team.id"
                class="flex items-center justify-between py-1 px-2 rounded hover:bg-surface-500"
              >
                <span class="text-sm text-gray-300">
                  {{ m.team.name }}
                  <span v-if="m.match !== 'exact'" class="text-yellow-400">({{ m.match }} match)</span>
                </span>
                <Checkbox 
                  :model-value="selected_teams.some(t => t.id === m.team.id)"
                  @update:model-value="toggleTeam(m.team)"
                  binary
                />
              </div>
            </div>
          </div>
        </div>
      </div>
      
      <div v-if="suggestedTeams.some(s => s.matches.length === 0)" class="text-sm text-yellow-400">
        Some teams were not found. You can add them manually.
      </div>
      
      <div v-if="duplicates.length > 0" class="text-lg font-bold p-2 m-2 bg-red-400 rounded-lg">
        {{ $t('teamSelect.warningAlreadyExistingAmalgamation') }}:
        {{ duplicates.map(it => it.name).join(", ") }}
      </div>
      
      <div class="flex gap-2 justify-between">
        <Button class="p-button-secondary p-button-rounded" @click="step = 'name'; selected_teams = []">Back</Button>
        <div class="flex gap-2">
          <Button class="p-button-outlined p-button-rounded" @click="goToManual">
            Manual Selection
          </Button>
          <Button 
            class="p-button-rounded" 
            @click="create_amalgamation"
            :disabled="selected_teams.length < 2"
            :loading="is_loading"
          >
            Create
          </Button>
        </div>
      </div>
    </div>
    
    <!-- Step 3: Manual selection (fallback) -->
    <div v-else-if="step === 'manual'" class="space-y-4">
      <div class="bg-surface-600 p-3 rounded-lg">
        <label for="amalgamation_name_manual" class="block mb-2">{{ $t('teamSelect.newAmalgamationName') }}</label>
        <InputText id="amalgamation_name_manual" v-model="amalgamation_name" class="w-full" />
      </div>
      
      <ul class="flex flex-row flex-wrap justify-center list-none w-full min-h-[4rem]">
        <li 
          v-for="team in selected_teams"
          :key="team.id"
          class="p-2 m-2 h-min rounded-2xl bg-surface-600 hover:bg-surface-500 hover:cursor-pointer inline-block"
          @click="selected_teams = selected_teams.filter(t => t.id !== team.id)"
        >
          {{ team.name }}
        </li>
      </ul>
      
      <TeamSelectField
        :exclude_team_list="selected_teams"
        :force_hide_exclude_team_list="true"
        :show_add_new_team="true"
        :show_new_amalgamate="false"
        @team_selected="on_team_selected"
        :show_hide_squad_box="true"
        :show_amalgamations="false"
      />
      
      <div v-if="duplicates.length > 0" class="text-lg font-bold p-2 m-2 bg-red-400 rounded-lg">
        {{ $t('teamSelect.warningAlreadyExistingAmalgamation') }}:
        {{ duplicates.map(it => it.name).join(", ") }}
      </div>
      
      <div class="flex gap-2 justify-between">
        <Button class="p-button-secondary p-button-rounded" @click="goToSuggestFromManual">Back to Suggestions</Button>
        <div class="flex gap-2">
          <Button class="p-button-secondary p-button-rounded" @click="cancel">{{ $t('general.cancel') }}</Button>
          <Button 
            class="p-button-rounded" 
            @click="create_amalgamation"
            :disabled="selected_teams.length < 2"
            :loading="is_loading"
          >
            {{ $t('general.submit') }}
          </Button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
</style>
