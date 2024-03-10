<script lang="ts" setup>
import {ref} from "vue";
import {useAdminStore} from "@/utils/admin_store";
import {useRoute, useRouter} from "vue-router";

const store = useAdminStore();
store.publicStore.loadAuxiliaryInformationFromSerer()
store.publicStore.loadTournaments()
store.publicStore.loadTeams()

const router = useRouter();


const items = ref([
  {
    label: "Referee Home",
    url: "/"
  }
    ,{
    label: "Report options",
    items: [
      {
        label: "Pitch options",
        route: "/pitch-options"
      },
      {
        label: "Rules",
        route: "/rules",
        icon: "pi pi-fw pi-book"
      },
      {
        label: "Game type & Extra Time options",
        icon: 'pi pi-fw pi-cog',
        route: "/game-report-options"
      }
    ],

  }, {
    label: "Tournament Reports",
    route: "/tournament-reports"
  }, {
    label: "Tournaments",
    route: "/tournaments"
  }, {
    label: "Teams",
    route: "/teams"
  }, {
    label: "Referees",
    route: "/referees"
  },{
    label: "CCC",
    route: "/ccc"
  }

])
const route = useRoute();


</script>

<template>
  <div>
    <Menubar :model="items" class="no-print" >
      <template #item="{ item, label, props, root, hasSubmenu }">
        <router-link v-if="item.route" v-slot="{ href, navigate }" :to="item.route" custom>
          <a v-ripple :href="href" v-bind="props.action" @click="navigate">
            <span :class="item.icon" />
            <span class="ml-2">{{ item.label }}</span>
          </a>
        </router-link>
        <a v-else v-ripple :href="item.url" :target="item.target" v-bind="props.action">
          <span :class="item.icon" />
          <span class="ml-2">{{ item.label }}</span>
          <span v-if="hasSubmenu" class="pi pi-fw pi-angle-down ml-2" />
        </a>
      </template>
    </Menubar>

  </div>
  <transition-group name="p-message" tag="div">
    <Message v-for="msg in store.currentErrors" severity="error" :key="msg.timestamp">{{msg.message}}</Message>
  </transition-group>
  <router-view></router-view>
</template>
<style>

</style>

<style scoped>

</style>

<style>
@media print {
  .no-print, .no-print * {
    display: none !important;
  }
}
</style>
