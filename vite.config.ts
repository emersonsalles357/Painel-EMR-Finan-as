import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

export default defineConfig({
  plugins: [react()],
  base: "/Painel-EMR-Finan-as/", // <-- Adicione esta linha exata
});
