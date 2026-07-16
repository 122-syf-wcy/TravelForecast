<template>
  <svg class="svg-icon" viewBox="0 0 24 24" aria-hidden="true" fill="none" xmlns="http://www.w3.org/2000/svg">
    <path
      v-for="path in iconPaths"
      :key="path"
      :d="path"
      stroke="currentColor"
      stroke-width="1.8"
      stroke-linecap="round"
      stroke-linejoin="round"
    />
  </svg>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  name?: string
}>()

const icons: Record<string, string[]> = {
  chart: ['M4 19V9', 'M10 19V5', 'M16 19v-8', 'M22 19H2'],
  'trend-up': ['M3 17l6-6 4 4 7-8', 'M14 7h6v6'],
  'trend-down': ['M3 7l6 6 4-4 7 8', 'M14 17h6v-6'],
  map: ['M9 18l-6 3V6l6-3 6 3 6-3v15l-6 3-6-3Z', 'M9 3v15', 'M15 6v15'],
  pin: ['M12 21s7-4.4 7-11a7 7 0 1 0-14 0c0 6.6 7 11 7 11Z', 'M12 10.5h.01'],
  building: ['M4 21V5l8-3 8 3v16', 'M9 21v-6h6v6', 'M8 8h.01', 'M12 8h.01', 'M16 8h.01', 'M8 12h.01', 'M16 12h.01'],
  mountain: ['M3 20h18L14 6l-4 7-2-3-5 10Z'],
  car: ['M5 16l1.5-5h11L19 16', 'M7 16h10', 'M7 20h.01', 'M17 20h.01', 'M4 16v3h16v-3'],
  bus: ['M6 3h12a2 2 0 0 1 2 2v11a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2Z', 'M4 10h16', 'M8 21v-3', 'M16 21v-3'],
  hotel: ['M4 21V5', 'M20 21V9a3 3 0 0 0-3-3H4', 'M4 13h16', 'M8 9h.01', 'M12 9h.01', 'M16 9h.01'],
  megaphone: ['M4 14h3l9 4V6l-9 4H4v4Z', 'M7 14l1 5'],
  bell: ['M18 8a6 6 0 0 0-12 0c0 7-3 7-3 9h18c0-2-3-2-3-9Z', 'M10 21h4'],
  phone: ['M6 3h12v18H6V3Z', 'M11 18h2'],
  bot: ['M8 9h8a4 4 0 0 1 4 4v4a3 3 0 0 1-3 3H7a3 3 0 0 1-3-3v-4a4 4 0 0 1 4-4Z', 'M12 9V5', 'M9 14h.01', 'M15 14h.01', 'M10 17h4'],
  sparkle: ['M12 3l1.8 5.2L19 10l-5.2 1.8L12 17l-1.8-5.2L5 10l5.2-1.8L12 3Z', 'M5 15l.8 2.2L8 18l-2.2.8L5 21l-.8-2.2L2 18l2.2-.8L5 15Z'],
  traffic: ['M10 2h4a3 3 0 0 1 3 3v14a3 3 0 0 1-3 3h-4a3 3 0 0 1-3-3V5a3 3 0 0 1 3-3Z', 'M12 7h.01', 'M12 12h.01', 'M12 17h.01'],
  shield: ['M12 3l7 3v5c0 5-3 8.5-7 10-4-1.5-7-5-7-10V6l7-3Z'],
  users: ['M16 19a4 4 0 0 0-8 0', 'M12 11a3 3 0 1 0 0-6 3 3 0 0 0 0 6Z', 'M20 19a3 3 0 0 0-3-3', 'M4 19a3 3 0 0 1 3-3'],
  user: ['M12 12a4 4 0 1 0 0-8 4 4 0 0 0 0 8Z', 'M4 21a8 8 0 0 1 16 0'],
  child: ['M12 8a3 3 0 1 0 0-6 3 3 0 0 0 0 6Z', 'M8 22v-8l-3-3', 'M16 22v-8l3-3', 'M8 14h8'],
  message: ['M4 5h16v11H8l-4 4V5Z'],
  heart: ['M20 8.5c0 5-8 10.5-8 10.5S4 13.5 4 8.5A4.5 4.5 0 0 1 12 6a4.5 4.5 0 0 1 8 2.5Z'],
  edit: ['M4 20h4l10-10a2.8 2.8 0 0 0-4-4L4 16v4Z', 'M13 7l4 4'],
  book: ['M5 4h10a4 4 0 0 1 4 4v12H8a3 3 0 0 0-3-3V4Z', 'M5 17V4'],
  clipboard: ['M9 4h6v4H9V4Z', 'M7 6H5v15h14V6h-2', 'M9 13h6', 'M9 17h4'],
  siren: ['M7 15v-3a5 5 0 0 1 10 0v3', 'M5 19h14', 'M9 3L8 5', 'M15 3l1 2', 'M3 10h2', 'M19 10h2'],
  medical: ['M12 4v16', 'M4 12h16'],
  search: ['M10 17a7 7 0 1 1 0-14 7 7 0 0 1 0 14Z', 'M15 15l6 6'],
  warning: ['M12 3l10 18H2L12 3Z', 'M12 9v5', 'M12 17h.01'],
  ticket: ['M4 7h16v4a2 2 0 0 0 0 4v2H4v-2a2 2 0 0 0 0-4V7Z', 'M9 9v6'],
  money: ['M4 7h16v10H4V7Z', 'M12 15a3 3 0 1 0 0-6 3 3 0 0 0 0 6Z'],
  lightbulb: ['M9 18h6', 'M10 22h4', 'M8 10a4 4 0 1 1 8 0c0 2-2 3-2 5h-4c0-2-2-3-2-5Z'],
  graduation: ['M3 8l9-4 9 4-9 4-9-4Z', 'M7 10v5c3 2 7 2 10 0v-5'],
  package: ['M4 8l8-4 8 4v8l-8 4-8-4V8Z', 'M4 8l8 4 8-4', 'M12 12v8'],
  food: ['M7 3v8', 'M10 3v8', 'M7 7h3', 'M17 3v18', 'M14 3c0 4 3 5 3 8'],
  flame: ['M12 22c4 0 7-3 7-7 0-4-3-7-5-10 0 3-2 4-4 6-1 1-2 3-2 5a4 4 0 0 0 4 6Z'],
  bolt: ['M13 2L4 14h7l-1 8 9-12h-7l1-8Z'],
  droplet: ['M12 3s6 7 6 11a6 6 0 0 1-12 0c0-4 6-11 6-11Z'],
  thermometer: ['M14 14.8V5a2 2 0 1 0-4 0v9.8a4 4 0 1 0 4 0Z'],
  sun: ['M12 8a4 4 0 1 0 0 8 4 4 0 0 0 0-8Z', 'M12 2v2', 'M12 20v2', 'M4.9 4.9l1.4 1.4', 'M17.7 17.7l1.4 1.4', 'M2 12h2', 'M20 12h2', 'M4.9 19.1l1.4-1.4', 'M17.7 6.3l1.4-1.4'],
  'cloud-sun': ['M8 17h9a4 4 0 0 0 0-8 5 5 0 0 0-9.8 1.2A3.5 3.5 0 0 0 8 17Z', 'M5 8a4 4 0 0 1 6-3.5'],
  cloud: ['M7 18h10a4 4 0 0 0 0-8 5 5 0 0 0-9.7 1.4A3.5 3.5 0 0 0 7 18Z'],
  rain: ['M7 17h10a4 4 0 0 0 0-8 5 5 0 0 0-9.7 1.4A3.5 3.5 0 0 0 7 17Z', 'M8 21l1-2', 'M12 21l1-2', 'M16 21l1-2'],
  snow: ['M12 3v18', 'M5 7l14 10', 'M19 7L5 17'],
  fog: ['M4 10h16', 'M6 14h12', 'M4 18h16'],
  wind: ['M3 8h11a3 3 0 1 0-3-3', 'M3 14h15a3 3 0 1 1-3 3', 'M3 20h9'],
  clock: ['M12 21a9 9 0 1 0 0-18 9 9 0 0 0 0 18Z', 'M12 7v5l3 2'],
  check: ['M4 12l5 5L20 6'],
  tool: ['M14 7l3-3 3 3-3 3', 'M14 7L5 16v3h3l9-9'],
  ban: ['M12 21a9 9 0 1 0 0-18 9 9 0 0 0 0 18Z', 'M5.6 5.6l12.8 12.8'],
  construction: ['M4 20h16', 'M6 20l4-12h4l4 12', 'M8 14h8'],
  star: ['M12 3l2.7 5.5 6.1.9-4.4 4.3 1 6.1L12 17l-5.4 2.8 1-6.1-4.4-4.3 6.1-.9L12 3Z'],
  'arrow-right': ['M5 12h14', 'M13 6l6 6-6 6'],
  free: ['M5 7h14v10H5V7Z', 'M8 12h8', 'M8 15h5'],
  sos: ['M5 8h14v8H5V8Z', 'M8 12h.01', 'M12 12h.01', 'M16 12h.01']
}

const normalizedName = computed(() => props.name || 'sparkle')
const iconPaths = computed(() => icons[normalizedName.value] || icons.sparkle)
</script>

<style scoped>
.svg-icon {
  width: 1em;
  height: 1em;
  display: inline-block;
  vertical-align: -0.125em;
}
</style>
