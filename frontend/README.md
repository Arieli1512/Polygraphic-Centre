# Polygraphic Centre Frontend

This frontend is a React + TypeScript + Vite application using React 19, TypeScript 6, and Vite 8.

## Requirements

- Node.js 18 or newer
- npm, Yarn, or pnpm
- Git (optional, if you clone the repository)
- Internet access to download dependencies

## Recommended setup

Install dependencies from the `frontend` folder using your preferred package manager. The project scripts use Vite and TypeScript.

### Linux

1. Install Node.js.
   - Ubuntu/Debian:
     ```bash
     sudo apt update
     sudo apt install nodejs npm
     ```
   - Fedora/RHEL:
     ```bash
     sudo dnf install nodejs npm
     ```
   - Alternatively, use Node Version Manager (nvm):
     ```bash
     curl -fsSL https://raw.githubusercontent.com/nvm-sh/nvm/v0.40.4/install.sh | bash
     source ~/.bashrc
     nvm install 20
     nvm use 20
     ```
2. Verify Node.js and npm versions:
   ```bash
   node -v
   npm -v
   ```
   Node should be 18+.

### macOS

1. Install Node.js via Homebrew:
   ```bash
   brew install node
   ```
   Or use nvm:
   ```bash
   curl -fsSL https://raw.githubusercontent.com/nvm-sh/nvm/v0.40.4/install.sh | bash
   source ~/.zshrc
   nvm install 20
   nvm use 20
   ```
2. Verify versions:
   ```bash
   node -v
   npm -v
   ```

### Windows

1. Download and install Node.js from https://nodejs.org/.
2. Ensure `npm` is available in PowerShell or Command Prompt.
3. Verify installation:
   ```powershell
   node -v
   npm -v
   ```

## Install dependencies

Open a terminal in the `frontend` folder.

### npm

```bash
cd /path/to/Polygraphic-Centre/frontend
npm install
```

### Yarn

```bash
cd /path/to/Polygraphic-Centre/frontend
yarn install
```

### pnpm

```bash
cd /path/to/Polygraphic-Centre/frontend
pnpm install
```

## Build and Run

### Start development server

```bash
npm run dev
```

or with Yarn:

```bash
yarn dev
```

or with pnpm:

```bash
pnpm dev
```

Open the local URL shown in the terminal (typically `http://localhost:5173`).

### Build production bundle

```bash
npm run build
```

### Preview production build

```bash
npm run preview
```

## Common scripts

- Start development server: `npm run dev`
- Build production assets: `npm run build`
- Preview production build: `npm run preview`
- Run ESLint: `npm run lint`

## Notes

- This project uses React 19 and TypeScript 6.
- If you see a Node.js version mismatch, confirm the `node` command points to Node 18+.
- Use the included package manager scripts rather than editing Vite configuration for standard local development.
- Copy [frontend/.env.example](.env.example) to a local `.env` file and fill values from the Firebase project settings and the backend URL you use locally or in deployment.
