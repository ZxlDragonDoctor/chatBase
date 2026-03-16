<template>
  <div class="app">
    <header class="topbar">
      <div class="brand">
        <div class="logo">CB</div>
        <div>
          <div class="title">ChatBase</div>
          <div class="sub">Web 控制台</div>
        </div>
      </div>
      <nav class="nav">
        <RouterLink class="link" to="/chat" active-class="active">聊天</RouterLink>
        <RouterLink class="link" to="/upload" active-class="active">上传</RouterLink>
      </nav>
    </header>
    <main class="main">
      <RouterView />
    </main>
  </div>
</template>

<script setup lang="ts"></script>

