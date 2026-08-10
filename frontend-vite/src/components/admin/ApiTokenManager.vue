<script lang="ts" setup>

import {useAdminStore} from "@/utils/admin_store";
import {createApiToken, listApiTokens, revokeApiToken} from "@/utils/api/admin_api";
import {onMounted, ref} from "vue";
import {useConfirm} from "primevue/useconfirm";
import {useToast} from "primevue/usetoast";
import type {ApiTokenCreatedDEO, ApiTokenDEO} from "@/types/api_token_types";

const store = useAdminStore();
const confirm = useConfirm();
const toast = useToast();

const tokens = ref<ApiTokenDEO[]>([]);
const isLoading = ref(false);

const newTokenName = ref("");
const newTokenExpiryInDays = ref<number | null>(null);
const creatingToken = ref(false);
const createdToken = ref<ApiTokenCreatedDEO | null>(null);

onMounted(() => {
  loadTokens()
})

function loadTokens() {
  isLoading.value = true
  listApiTokens()
      .then(it => tokens.value = it)
      .catch(err => store.newError(err))
      .finally(() => isLoading.value = false)
}

function createToken() {
  if (!newTokenName.value) {
    return
  }
  creatingToken.value = true
  createApiToken({
    name: newTokenName.value,
    expiresInDays: newTokenExpiryInDays.value
  })
      .then(token => {
        createdToken.value = token
        newTokenName.value = ""
        newTokenExpiryInDays.value = null
        loadTokens()
      })
      .catch(err => store.newError(err))
      .finally(() => creatingToken.value = false)
}

function copyTokenToClipboard() {
  if (!createdToken.value) {
    return
  }
  navigator.clipboard.writeText(createdToken.value.token)
      .then(() => newSuccessMessage("Token copied to clipboard. It will not be shown again."))
      .catch(() => store.newError("Could not copy token to clipboard"))
}

function dismissTokenDisplay() {
  createdToken.value = null
}

function confirmRevoke(token: ApiTokenDEO) {
  confirm.require({
    group: "apiTokenManager",
    message: `Are you sure you want to revoke this token (${token.name})?`,
    header: "Revoke Confirmation",
    icon: 'pi pi-info-circle',
    acceptClass: 'p-button-danger',
    accept: () => {
      revokeApiToken(token.id)
          .then(() => loadTokens())
          .catch(err => store.newError(err))
    },
    reject: () => {
      //Rejected revoke
    }
  })
}

function formatDate(date: string | null): string {
  if (!date) {
    return "-"
  }
  return date.replace("T", " ").substring(0, 16)
}

function isExpired(token: ApiTokenDEO): boolean {
  return !!token.expiresAt && new Date(token.expiresAt).getTime() < Date.now()
}

function statusLabel(token: ApiTokenDEO): string {
  if (token.revoked) {
    return "Revoked"
  }
  if (isExpired(token)) {
    return "Expired"
  }
  return "Active"
}

function newSuccessMessage(message: string) {
  toast.add({
    severity: 'success',
    summary: 'Success',
    detail: message,
    life: 3000
  });
}

</script>

<template>
  <div class="flex flex-col items-center w-full">
    <Toast />
    <h3>API Tokens</h3>
    <div class="w-full lg:w-3/4 xl:w-1/2">
      <div class="card p-4 mb-4">
        <h5>Create new API token</h5>
        <div class="flex flex-col gap-3">
          <div class="flex flex-col">
            <label for="token-name">Name</label>
            <InputText id="token-name" v-model="newTokenName" placeholder="e.g. MCP server access"/>
          </div>
          <div class="flex flex-col">
            <label for="token-expiry">Expires in days (optional)</label>
            <InputNumber id="token-expiry" v-model="newTokenExpiryInDays" :min="1" placeholder="Leave empty for no expiry"/>
          </div>
          <div class="flex justify-end">
            <Button
                label="Create Token"
                icon="pi pi-plus"
                class="p-button-success"
                :loading="creatingToken"
                :disabled="!newTokenName"
                @click="createToken"
            />
          </div>
        </div>
      </div>

      <Message
          v-if="createdToken"
          severity="warn"
          :closable="false"
          class="mb-4"
      >
        <div class="flex flex-col gap-2">
          <span><b>Store this token now.</b> It is shown only once and cannot be retrieved again. Anyone with this token has admin API access.</span>
          <div class="flex flex-row gap-2 align-items-center">
            <InputText :model-value="createdToken.token" readonly class="flex-grow-1" style="font-family: monospace"/>
            <Button
                icon="pi pi-copy"
                label="Copy"
                class="p-button-sm"
                @click="copyTokenToClipboard"
            />
            <Button
                icon="pi pi-times"
                label="Dismiss"
                class="p-button-sm p-button-secondary"
                @click="dismissTokenDisplay"
            />
          </div>
        </div>
      </Message>

      <div v-if="isLoading" class="flex flex-row justify-center m-4">
        <i class="pi pi-spin pi-spinner text-2xl"></i>
      </div>
      <DataTable v-else :value="tokens" class="w-full mt-2">
        <Column field="name" header="Name" sortable/>
        <Column field="createdAt" header="Created" sortable>
          <template #body="{data}:{data: ApiTokenDEO}">
            {{ formatDate(data.createdAt) }}
          </template>
        </Column>
        <Column field="expiresAt" header="Expires" sortable>
          <template #body="{data}:{data: ApiTokenDEO}">
            {{ formatDate(data.expiresAt) }}
          </template>
        </Column>
        <Column field="lastUsedAt" header="Last Used" sortable>
          <template #body="{data}:{data: ApiTokenDEO}">
            {{ formatDate(data.lastUsedAt) }}
          </template>
        </Column>
        <Column field="revoked" header="Status">
          <template #body="{data}:{data: ApiTokenDEO}">
            <span
                :class="{
                  'text-red-600 font-bold': data.revoked,
                  'text-yellow-600 font-bold': !data.revoked && isExpired(data),
                  'text-green-600 font-bold': !data.revoked && !isExpired(data)
                }"
            >{{ statusLabel(data) }}</span>
          </template>
        </Column>
        <Column header="Actions" :style="{width: '8rem'}">
          <template #body="{data}:{data: ApiTokenDEO}">
            <Button
                v-if="!data.revoked"
                icon="pi pi-ban"
                label="Revoke"
                class="p-button-rounded p-button-danger text-sm"
                @click="confirmRevoke(data)"
            />
          </template>
        </Column>
      </DataTable>
    </div>
    <ConfirmDialog group="apiTokenManager"></ConfirmDialog>
  </div>
</template>

<style scoped>
h3 {
  @apply m-2;
  @apply text-xl font-bold;
  @apply text-center;
}
</style>
