<template>
  <div id="app-root">
    <nav class="nav-bar">
      <router-link to="/" class="nav-logo">🎤 TOEIC Speaking</router-link>
      <div class="nav-links">
        <router-link to="/">홈</router-link>
        <router-link to="/practice">연습하기</router-link>
        <router-link to="/full-test">모의고사</router-link>
        <router-link to="/my-records">내 기록</router-link>
        <span v-if="store.currentUser" class="user-info">{{ store.currentUser.nickname }}님</span>
      </div>
    </nav>
    <main class="main-content">
      <router-view />
    </main>
  </div>
</template>

<script setup>
import { useSpeakingStore } from './stores/speaking'
const store = useSpeakingStore()
</script>

<style scoped>
.nav-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  height: 60px;
  background: #4A90D9;
  color: #fff;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
}

.nav-logo {
  font-size: 1.3rem;
  font-weight: 700;
  color: #fff;
  text-decoration: none;
}

.nav-links {
  display: flex;
  gap: 20px;
}

.nav-links a {
  color: rgba(255,255,255,0.85);
  text-decoration: none;
  font-weight: 500;
  padding: 6px 12px;
  border-radius: 6px;
  transition: background 0.2s;
}

.nav-links a:hover,
.nav-links a.router-link-active {
  background: rgba(255,255,255,0.2);
  color: #fff;
}

.user-info {
  color: #fff;
  font-weight: 600;
  background: rgba(255,255,255,0.2);
  padding: 4px 12px;
  border-radius: 16px;
  font-size: 0.9rem;
}

.main-content {
  max-width: 900px;
  margin: 0 auto;
  padding: 24px 16px;
}
</style>
