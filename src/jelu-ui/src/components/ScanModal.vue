<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { useI18n } from 'vue-i18n';
import { DetectedBarcode, EmittedError, QrcodeStream } from 'vue-qrcode-reader';
import MDialog from "./m3/MDialog.vue";
import MSelect from "./m3/MSelect.vue";

const { t } = useI18n({
      inheritLocale: true,
      useScope: 'global'
    })

const emit = defineEmits<{
  (e: 'close'): void,
  (e: 'decoded', barcode: string|null): void,
  (e: 'barcodeLoaded', reader: any): void
}>()

const decodedText = ref("");
const barcodeReader = ref()
const loading = ref(true)

const selected = ref(null as MediaDeviceInfo | null)
const devices = ref([] as MediaDeviceInfo[])
const devicesReady = ref(false)

const torchActive = ref(false)
const torchNotSupported = ref(false)

// name of the last camera error, shown as a banner until the camera starts
const cameraError = ref(null as string | null)

const cameraErrorMessage = computed(() => {
  switch (cameraError.value) {
    case 'NotAllowedError':
      return t('scan.camera_denied')
    case 'NotFoundError':
    case 'OverconstrainedError':
      return t('scan.camera_not_found')
    case 'InsecureContextError':
      return t('scan.camera_insecure')
    default:
      return t('scan.camera_error')
  }
})

// remembers the user's camera choice across modal openings
const STORAGE_KEY = 'jelu.scanner.deviceId'

// upstream default (facingMode: environment) unless the user picked a device
const constraints = computed(() => {
  if (selected.value?.deviceId) {
    return {
      deviceId: { exact: selected.value.deviceId },
      width: { ideal: 1920 },
      height: { ideal: 1080 }
    }
  }
  return {
    facingMode: 'environment',
    width: { ideal: 1920 },
    height: { ideal: 1080 }
  }
})

// draws a frame around detected codes
const paintOutline = (detectedCodes: Array<DetectedBarcode>, ctx: CanvasRenderingContext2D) => {
  for (const detectedCode of detectedCodes) {
    const [firstPoint, ...otherPoints] = detectedCode.cornerPoints
    if (!firstPoint) continue

    ctx.strokeStyle = "#22cc55"
    ctx.lineWidth = 4

    ctx.beginPath()
    ctx.moveTo(firstPoint.x, firstPoint.y)
    for (const { x, y } of otherPoints) {
      ctx.lineTo(x, y)
    }
    ctx.lineTo(firstPoint.x, firstPoint.y)
    ctx.closePath()
    ctx.stroke()
  }
}

const acceptBarcode = () => {
    emit('decoded', decodedText.value)
    emit('close')
}

const loadDevices = async () => {
  devices.value = (await navigator.mediaDevices.enumerateDevices()).filter(
    ({ kind }) => kind === 'videoinput'
  )
}

// apply the saved camera once labels are available; drop a stale saved id
const applyStoredDevice = () => {
  if (devices.value.length === 0 || devices.value.some((d) => !d.label)) return
  let storedId = null as string | null
  try {
    storedId = localStorage.getItem(STORAGE_KEY)
  } catch (e) {
    console.log(e)
  }
  if (!storedId) return
  const match = devices.value.find((d) => d.deviceId === storedId)
  if (match) {
    selected.value = match
  } else {
    try {
      localStorage.removeItem(STORAGE_KEY)
    } catch (e) {
      console.log(e)
    }
  }
}

// save the user's camera pick; never write from script
const onDeviceChange = () => {
  if (!selected.value?.deviceId) return
  try {
    localStorage.setItem(STORAGE_KEY, selected.value.deviceId)
  } catch (e) {
    console.log(e)
  }
}

// eslint-disable-next-line no-undef
const onLoaded = async (capabilities: Partial<MediaTrackCapabilities>) => {
  console.log("barcode modal loaded");
  console.log(capabilities)
  torchNotSupported.value = !(capabilities as any).torch
  loading.value = false
  cameraError.value = null
  if (devices.value.length === 0 || devices.value.some((d) => !d.label)) {
    try {
      // labels are empty before permission is granted; refresh now that it is
      await loadDevices()
    } catch (error) {
      console.log(error)
    }
    if (selected.value && !devices.value.find((d) => d.deviceId === selected.value?.deviceId)) {
      selected.value = null
    }
  }
  emit('barcodeLoaded', barcodeReader.value)
};

const onDecode = (detectedBarcodes: Array<DetectedBarcode>) => {
  console.log("barcode ");
  console.log(detectedBarcodes)
  decodedText.value = detectedBarcodes[0].rawValue
  acceptBarcode()
};

const onError = (error: EmittedError) => {
  console.log("barcode reader error")
  console.log(error)
  cameraError.value = error.name
  // avoid a permanent lock on a bad saved id
  try {
    localStorage.removeItem(STORAGE_KEY)
  } catch (e) {
    console.log(e)
  }
}

onMounted(async () => {
  try {
    await loadDevices()
  } finally {
    devicesReady.value = true
  }
  applyStoredDevice()
})
</script>

<template>
  <m-dialog
    :headline="t('labels.scan_isbn')"
    size="md"
    @close="emit('close')"
  >
    <div class="flex flex-col gap-4">
      <m-select
        v-if="devices.length > 0"
        v-model="selected"
        :label="t('labels.pick_camera')"
        leading-icon="camera-outline"
        @change="onDeviceChange"
      >
        <option
          v-for="(device, index) in devices"
          :key="device.deviceId || index"
          :value="device"
        >
          {{ device.label }}
        </option>
      </m-select>
      <div class="relative aspect-[4/3] w-full overflow-hidden rounded-lg bg-surface-container-highest">
        <qrcode-stream
          v-if="devicesReady"
          ref="barcodeReader"
          :constraints="constraints"
          :torch="torchActive"
          :track="paintOutline"
          :formats="['qr_code', 'ean_13']"
          @detect="onDecode"
          @camera-on="onLoaded"
          @error="onError"
        >
          <div
            v-if="cameraError != null"
            class="grid h-full place-items-center text-on-surface-variant"
          >
            <i
              class="mdi mdi-camera-off-outline text-[3rem] leading-none"
              aria-hidden="true"
            />
          </div>
          <div
            v-else-if="loading"
            class="grid h-full place-items-center"
          >
            <span
              class="m3-spinner m3-spinner--lg"
              role="progressbar"
              :aria-label="t('labels.loading')"
            />
          </div>
          <button
            v-else-if="!torchNotSupported"
            v-tooltip="t('scan.torch')"
            type="button"
            class="m3-icon-btn m3-icon-btn--tonal absolute right-3 bottom-3"
            :class="{ 'is-selected': torchActive }"
            :aria-pressed="torchActive"
            :aria-label="t('scan.torch')"
            @click="torchActive = !torchActive"
          >
            <i
              class="mdi"
              :class="torchActive ? 'mdi-flashlight' : 'mdi-flashlight-off'"
              aria-hidden="true"
            />
          </button>
        </qrcode-stream>
        <div
          v-else
          class="grid h-full place-items-center"
        >
          <span
            class="m3-spinner m3-spinner--lg"
            role="progressbar"
            :aria-label="t('labels.loading')"
          />
        </div>
      </div>
      <p
        v-if="cameraError != null"
        class="flex items-center gap-3 rounded-md bg-error-container p-4 text-body-medium text-on-error-container"
        role="alert"
      >
        <i
          class="mdi mdi-alert-circle-outline text-[1.25rem]"
          aria-hidden="true"
        />
        <span>{{ cameraErrorMessage }}</span>
      </p>
      <p
        v-if="decodedText"
        class="flex items-center gap-3 rounded-md bg-secondary-container p-4 text-body-medium text-on-secondary-container"
        role="status"
      >
        <i
          class="mdi mdi-barcode text-[1.25rem]"
          aria-hidden="true"
        />
        <span class="tabular-nums">{{ decodedText }}</span>
      </p>
    </div>
    <template #actions>
      <button
        type="button"
        class="m3-btn m3-btn--text"
        @click="emit('close')"
      >
        {{ t('labels.cancel') }}
      </button>
    </template>
  </m-dialog>
</template>
