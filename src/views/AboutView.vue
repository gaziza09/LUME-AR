<template>
  <div class="vosk-client">
    <header class="animate__animated animate__fadeInDown">
      <h1><span class="lume">Lume</span><span class="ar">Ar</span></h1>
      <p class="subtitle">Анализ звуков природы</p>
    </header>

    <!-- Основной интерфейс -->
    <div class="main-content">
      <!-- Интерфейс записи -->
      <div class="recording-section card animate__animated animate__fadeIn">
        <div class="button-group">
          <button @click="requestPermission" class="btn record" :class="{ recording: isRecording }">
            {{ isRecording ? "Остановить запись" : "Начать запись" }}
            <span class="mic-icon" :class="{ active: isRecording }">🎙️</span>
          </button>
          <button @click="goBack" class="btn secondary animate__animated animate__bounceIn">Назад</button>
        </div>
        <p v-if="error" class="error animate__animated animate__shakeX">{{ error }}</p>
      </div>

      <!-- Блок состояния -->
      <div class="status-section card animate__animated animate__fadeIn">
        <h2>Состояние</h2>
        <p><strong>WebSocket (звуки):</strong> {{ wsStatus }}</p>
        <p><strong>Микрофон:</strong> {{ micStatus }}</p>
      </div>

      <!-- История распознавания звуков -->
      <div class="history-section card animate__animated animate__fadeIn">
        <h2>История звуков</h2>
        <ul v-if="natureSounds.length > 0" class="history-list">
          <li v-for="(sound, index) in natureSounds" :key="index" class="history-item animate__animated animate__fadeInUp">
            {{ sound.name }} (уверенность: {{ (sound.confidence * 100).toFixed(2) }}%)
          </li>
        </ul>
        <p v-else class="no-history">Звуки не обнаружены</p>
      </div>

      <!-- О звуках природы -->
      <div class="about-section card animate__animated animate__fadeIn">
        <h2>О звуках природы</h2>
        <p>LumeAr анализирует звуки окружающей среды и распознает природные звуки в реальном времени с помощью передовых технологий обработки аудио.</p>
      </div>
    </div>

    <!-- Ссылка на GitHub -->
    <footer class="github-link animate__animated animate__fadeInUp animate__delay-1s">
      <a href="https://github.com/gaziza09/LUME-AR" target="_blank">Check out on GitHub</a>
    </footer>
  </div>
</template>

<script>
export default {
  data() {
    return {
      wsSounds: null,
      isRecording: false,
      audioContext: null,
      error: null,
      natureSounds: [],
      wsStatus: "Отключен",
    };
  },
  computed: {
    micStatus() {
      return this.isRecording ? "Активен" : "Неактивен";
    },
  },
  methods: {
    goBack() {
      this.$router.push("/");
    },

    async requestPermission() {
      try {
        const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
        stream.getTracks().forEach((track) => track.stop());
        this.toggleRecording();
      } catch (error) {
        this.error = error.message;
        console.error("Доступ к микрофону отклонён:", error);
      }
    },

    toggleRecording() {
      if (this.isRecording) {
        this.stopRecording();
      } else {
        this.audioContext = new (window.AudioContext || window.webkitAudioContext)();
        this.startRecording();
      }
    },

    async startRecording() {
      try {
        const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
        const input = this.audioContext.createMediaStreamSource(stream);
        const processor = this.audioContext.createScriptProcessor(8192, 1, 1);

        this.wsSounds = new WebSocket(`wss://easywork.kz/nature_sounds`);
        this.wsSounds.binaryType = "arraybuffer";

        this.wsSounds.onopen = () => {
          this.wsStatus = "Подключен (звуки)";
          console.log("WebSocket для звуков природы подключен");
        };

        this.wsSounds.onmessage = (event) => {
          const data = JSON.parse(event.data);
          if (data.predicted_class) {
            this.natureSounds.unshift({
              name: data.predicted_class,
              confidence: 1.0,
            });
            if (this.natureSounds.length > 5) this.natureSounds.pop();
          }
        };

        this.wsSounds.onerror = (error) => {
          this.wsStatus = "Ошибка (звуки)";
          console.error("Ошибка WebSocket звуков:", error);
        };

        this.wsSounds.onclose = () => {
          this.wsStatus = "Отключен (звуки)";
          console.log("WebSocket звуков закрыт.");
        };

        const downsampleBuffer = (buffer, targetSampleRate, sourceSampleRate) => {
          const ratio = sourceSampleRate / targetSampleRate;
          const length = Math.round(buffer.length / ratio);
          const result = new Int16Array(length);
          let offset = 0;
          let inputOffset = 0;

          while (offset < result.length) {
            const nextOffset = Math.round((offset + 1) * ratio);
            let accumulator = 0;
            let count = 0;

            for (let i = inputOffset; i < nextOffset && i < buffer.length; i++) {
              accumulator += buffer[i];
              count++;
            }

            result[offset] = Math.min(1, accumulator / count) * 0x7FFF;
            offset++;
            inputOffset = nextOffset;
          }

          return result;
        };

        processor.onaudioprocess = (event) => {
          const inputData = event.inputBuffer.getChannelData(0);
          const downsampled = downsampleBuffer(inputData, 16000, this.audioContext.sampleRate);
          const audioBuffer = downsampled.buffer;

          if (this.wsSounds.readyState === WebSocket.OPEN) {
            this.wsSounds.send(audioBuffer);
          }
        };

        input.connect(processor);
        processor.connect(this.audioContext.destination);

        this.isRecording = true;
        console.log("Запись началась...");
      } catch (error) {
        console.error("Ошибка доступа к микрофону:", error);
        this.error = error.message;
      }
    },

    stopRecording() {
      if (this.isRecording) {
        if (this.wsSounds && this.wsSounds.readyState === WebSocket.OPEN) {
          this.wsSounds.close();
          console.log("WebSocket для звуков природы закрыт.");
        }

        if (this.audioContext) {
          this.audioContext.close().then(() => {
            console.log("Аудиоконтекст закрыт.");
          });
        }

        this.isRecording = false;
        console.log("Запись остановлена.");
      }
    },
  },
};
</script>

<style>
/* Стили остаются такими же, как в основном компоненте */
</style>