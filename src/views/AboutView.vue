<template>
  <div class="proxy-client">
    <h1>Proxy WebSocket Client</h1>
    <p v-if="proxyMessage"><strong>Сообщение от Proxy:</strong> {{ proxyMessage }}</p>
    <p v-if="error" class="error">Ошибка: {{ error }}</p>
  </div>
</template>

<script>
export default {
  data() {
    return {
      proxyWs: null,
      proxyMessage: "",
      error: null,
    };
  },
  mounted() {
    this.connectToProxy();
  },
  methods: {
    connectToProxy() {
      try {
        this.proxyWs = new WebSocket("wss://10.8.16.162:8000/proxy");
        
        this.proxyWs.onopen = () => {
          console.log("Подключено к Proxy WebSocket.");
        };

        this.proxyWs.onmessage = (event) => {
          const data = JSON.parse(event.data);
          if (data.message) {
            this.proxyMessage = data.message;
          } else if (data.text) {
            this.proxyMessage = data.text;
          } else {
            this.proxyMessage = "Нет данных.";
          }
        };

        this.proxyWs.onerror = (error) => {
          this.error = "Ошибка соединения с Proxy WebSocket.";
          console.error("WebSocket Proxy Error:", error);
        };

        this.proxyWs.onclose = () => {
          console.log("Соединение с Proxy WebSocket закрыто.");
        };
      } catch (error) {
        this.error = "Не удалось подключиться к Proxy WebSocket.";
        console.error(error);
      }
    },
  },
};
</script>

<style>
.proxy-client {
  max-width: 500px;
  margin: 50px auto;
  text-align: center;
  font-family: Arial, sans-serif;
}

p {
  margin-top: 20px;
  font-size: 1.2em;
}

.error {
  color: red;
  font-size: 1em;
}
</style>
