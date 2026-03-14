<script setup lang="ts">
import { onLaunch, onShow, onHide } from "@dcloudio/uni-app";
onLaunch(() => {
  console.log("App Launch");
});
onShow(() => {
  console.log("App Show");
});
onHide(() => {
  console.log("App Hide");
});
</script>
<style lang="scss">
@import 'uview-plus/index.scss';

:root {
  --primary: #00bebd;
  --text: #1f2329;
  --bg: #f6f7fb;
  --card: #ffffff;
  --muted: #646a73;
  --border: rgba(31, 35, 41, 0.12);
}

page {
  background-color: var(--bg);
  color: var(--text);
  font-family: ui-sans-serif, system-ui, -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, 'Apple Color Emoji', 'Segoe UI Emoji';
}
</style>
