<template>
  <div id="app">
    <ul id="nav">
      <li><router-link to="/">JvmSearch</router-link></li>
      <li class="right"><router-link to="/login">Login</router-link></li>
      <li class="right"><router-link to="/demos">Demos</router-link></li>
    </ul>
    <div id="content">
      <router-view/>
    </div>
    <div id="footer">
      <div id="separator"></div>
      <div class="content">
        Created by <a class="mono" title="Kotlin, Java, etc | Hire me!" href="https://github.com/mktiti" target="_blank">@mktiti</a>
        <span>
          <a class="icon" title="LinkedIn" target="_blank" href="https://www.linkedin.com/in/kriszti%C3%A1n-m%C3%A1rkus/"><fa-icon :icon="['fab', 'linkedin']" /></a>
          <a class="icon" title="GitHub" target="_blank" href="https://github.com/mktiti/FunctionSearch"><fa-icon :icon="['fab', 'github']" /></a>
        </span>
        <br>
        Written in
        <a href="https://kotlinlang.org/" target="_blank">Kotlin</a>&
        <a href="https://vuejs.org/" target="_blank">Vue.js</a>
      </div>
    </div>
  </div>
</template>

<style>
body {
  background: #2a2a2e;
  padding: 0;
  margin: 0;
  height: 100vh;
}

#app {
  font-family: Avenir, Helvetica, Arial, serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  margin: 0;
  padding: 0;
  height: 100%;
  display: flex;
  flex-direction: column;
}

#nav {
  margin: 0;
  padding: 0;
  top: 0;
  background: #1d89ff;
  width: 100%;
  z-index: 10;
  list-style-type: none;
  border-bottom: 5px solid #8bbeff;
}

#nav li {
  float: left;
}

#nav li.right {
  float: right;
}

#nav li a {
  display: block;
  text-decoration: none;
  color: white;
  font-size: 1.3rem;
  font-weight: 600;
  letter-spacing: 1px;
  padding: 17px 15px 10px 20px;
}

#nav li.right:hover, #nav li.right:focus {
  background-color: #32337d;
}

@media screen and (max-width: 350px) {
  #nav li.right,
  #nav li {
    float: none;
    padding-left: 15px;
  }
}

#footer {
  margin-top: auto;
  width: 100%;
}

#footer>#separator {
  width: 90%;
  box-shadow: 0 0 0 1px #324e6d;
  margin: auto;
  border: 1px solid #324e6d;
  border-radius: 5px;
}

#footer>.content {
  padding: 10px;
  text-align: center;
  color: #e6e6e6;
  line-height: 1.8;
}

#footer>.content a.mono {
  font-family: monospace;
  font-size: 15px;
}

#footer>.content a.icon {
  font-family: monospace;
  font-size: 25px;
  vertical-align: sub;
  margin: 5px;
}

#footer>.content a {
  text-decoration: none;
  color: #e6e6e6;
  margin-right: 5px;
}

#footer>.content a:hover {
  color: #476a9a;
}
</style>
