<template>
  <div class="vosk-client">
    <header class="animate__animated animate__fadeInDown">
      <h1><span class="lume">Lume</span><span class="ar">Ar</span></h1>
      <p class="subtitle">Voice Recognition System</p>
    </header>

    <!-- Выбор языка -->
    <div v-if="!languageSelected" class="language-selection card animate__animated animate__fadeIn">
      <label for="language">Выберите язык:</label>
      <select v-model="selectedLanguage" id="language" class="language-select">
        <option value="KZ">Казахский (KZ)</option>
        <option value="RU">Русский (RU)</option>
        <option value="EN">Английский (EN)</option>
      </select>
      <button @click="confirmLanguage"
        class="btn primary animate__animated animate__pulse animate__infinite">Подтвердить</button>
    </div>

    <!-- Основной интерфейс -->
    <div v-if="languageSelected" class="main-content">
      <!-- Интерфейс записи -->
      <div class="recording-section card animate__animated animate__fadeIn">
        <div class="button-group">
          <button @click="requestPermission" class="btn record" :class="{ recording: isRecording }">
            {{ isRecording ? "Остановить запись" : "Начать запись" }}
            <span class="mic-icon" :class="{ active: isRecording }">🎙️</span>
          </button>
          <button @click="changeLanguage" class="btn secondary animate__animated animate__bounceIn">Сменить
            язык</button>
          <button @click="goToAbout" class="btn secondary animate__animated animate__bounceIn">Nature sounds</button>
        </div>
        <p v-if="result" class="result-text animate__animated animate__fadeInUp"><strong>Распознанный текст:</strong> {{
          result }}</p>
        <p v-if="error" class="error animate__animated animate__shakeX">{{ error }}</p>
      </div>

      <!-- Блок состояния -->
      <div class="status-section card animate__animated animate__fadeIn">
        <h2>Состояние</h2>
        <p><strong>WebSocket (речь):</strong> {{ wsStatus }}</p>
        <p><strong>Микрофон:</strong> {{ micStatus }}</p>
        <p><strong>Язык:</strong> {{ selectedLanguage }}</p>
      </div>

      <!-- История распознавания -->
      <div class="history-section card animate__animated animate__fadeIn">
        <h2>История</h2>
        <ul v-if="history.length > 0" class="history-list">
          <li v-for="(item, index) in history" :key="index" class="history-item animate__animated animate__fadeInUp">
            {{ item }}
          </li>
        </ul>
        <p v-else class="no-history">Пока ничего не распознано</p>
      </div>

      <!-- О проекте -->
      <div class="about-section card animate__animated animate__fadeIn">
        <h2>О LumeAr</h2>
        <p>LumeAr — это инновационная система распознавания голоса, разработанная для упрощения взаимодействия с
          технологиями. Поддерживает несколько языков и работает в реальном времени.</p>
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
      wsSpeech: null,
      result: "",
      isRecording: false,
      mediaRecorder: null,
      permissionDenied: false,
      error: null,
      audioContext: null,
      selectedLanguage: "KZ",
      languageSelected: false,
      history: [],
      wsStatus: "Отключен",
    };
  },
  computed: {
    micStatus() {
      return this.isRecording ? "Активен" : "Неактивен";
    },
  },
  methods: {
    confirmLanguage() {
      if (this.selectedLanguage) {
        this.languageSelected = true;
        console.log(`Выбран язык: ${this.selectedLanguage}`);
      }
    },

    changeLanguage() {
      this.stopRecording();
      this.languageSelected = false;
      this.result = "";
      this.error = null;
      console.log("Смена языка инициирована");
    },

    goToAbout() {
      this.$router.push("/about");
    },

    async requestPermission() {
      try {
        const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
        stream.getTracks().forEach((track) => track.stop());
        this.permissionDenied = false;
        this.toggleRecording();
      } catch (error) {
        this.permissionDenied = true;
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

        this.wsSpeech = new WebSocket(`wss://easywork.kz/ws?lang=${this.selectedLanguage}`);
        this.wsSpeech.binaryType = "arraybuffer";

        this.wsSpeech.onopen = () => {
          this.wsStatus = "Подключен (речь)";
          console.log("WebSocket для речи подключен");
        };

        this.wsSpeech.onmessage = (event) => {
          const data = JSON.parse(event.data);
          this.result = data.text || data.partial || "Нет распознанного текста.";
          if (data.text) {
            this.history.unshift(data.text);
            if (this.history.length > 5) this.history.pop();
          }
        };

        this.wsSpeech.onerror = (error) => {
          this.wsStatus = "Ошибка (речь)";
          console.error("Ошибка WebSocket речи:", error);
        };

        this.wsSpeech.onclose = () => {
          this.wsStatus = "Отключен (речь)";
          console.log("WebSocket речи закрыт.");
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

          if (this.wsSpeech.readyState === WebSocket.OPEN) {
            this.wsSpeech.send(audioBuffer);
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
        this.wsSpeech.close();
        this.audioContext.close();
        this.isRecording = false;
        console.log("Запись остановлена.");
      }
    },
  },
};
</script>

