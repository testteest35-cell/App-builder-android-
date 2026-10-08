/**
 * DroidIDE Remote Cloud Build Server
 * Node.js / Express Build Pipeline Engine
 */

const express = require('express');
const multer = require('multer');
const fs = require('fs');
const path = require('path');
const { spawn } = require('child_process');
const unzipper = require('unzipper');
const { v4: uuidv4 } = require('uuid');
const cors = require('cors');

const app = express();
const PORT = process.env.PORT || 3000;

app.use(cors());
app.use(express.json());

const UPLOAD_DIR = path.join(__dirname, 'build_workspaces');
if (!fs.existsSync(UPLOAD_DIR)) {
  fs.mkdirSync(UPLOAD_DIR, { recursive: true });
}

const storage = multer.diskStorage({
  destination: (req, file, cb) => cb(null, UPLOAD_DIR),
  filename: (req, file, cb) => cb(null, `${uuidv4()}_${file.originalname}`)
});
const upload = multer({ storage });

// In-memory job state store
const jobs = new Map();

/**
 * POST /api/v1/build
 * Accepts multipart/form-data containing projectZip, projectName, packageName
 */
app.post('/api/v1/build', upload.single('projectZip'), async (req, res) => {
  try {
    if (!req.file) {
      return res.status(400).json({ error: 'Missing projectZip file' });
    }

    const jobId = uuidv4();
    const projectName = req.body.projectName || 'App';
    const workspaceDir = path.join(UPLOAD_DIR, jobId);

    fs.mkdirSync(workspaceDir, { recursive: true });

    const job = {
      jobId,
      projectName,
      workspaceDir,
      status: 'QUEUED',
      logs: [`[Server] Received project archive: ${req.file.originalname}`],
      apkPath: null,
      createdAt: new Date().toISOString()
    };
    jobs.set(jobId, job);

    res.status(202).json({
      jobId,
      status: 'QUEUED',
      message: 'Build job created successfully'
    });

    // Asynchronously extract and execute Gradle build
    processBuildJob(job, req.file.path);
  } catch (err) {
    console.error('Error initiating build:', err);
    res.status(500).json({ error: err.message });
  }
});

async function processBuildJob(job, uploadedZipPath) {
  try {
    job.status = 'EXTRACTING';
    job.logs.push('[Server] Extracting project files...');

    // Extract ZIP archive into workspace
    await fs.createReadStream(uploadedZipPath)
      .pipe(unzipper.Extract({ path: job.workspaceDir }))
      .promise();

    fs.unlinkSync(uploadedZipPath); // Clean up upload zip

    job.status = 'BUILDING';
    job.logs.push('[Server] Starting Gradle build: ./gradlew assembleDebug');

    // Run Gradle daemon inside workspace
    const gradlewPath = path.join(job.workspaceDir, 'gradlew');
    const hasGradlew = fs.existsSync(gradlewPath);

    if (hasGradlew) {
      fs.chmodSync(gradlewPath, '755');
    }

    const buildCmd = hasGradlew ? './gradlew' : 'gradle';
    const buildArgs = ['assembleDebug', '--no-daemon', '--stacktrace'];

    const gradleProcess = spawn(buildCmd, buildArgs, {
      cwd: job.workspaceDir,
      env: { ...process.env }
    });

    gradleProcess.stdout.on('data', (data) => {
      const line = data.toString().trim();
      if (line) {
        job.logs.push(line);
      }
    });

    gradleProcess.stderr.on('data', (data) => {
      const line = data.toString().trim();
      if (line) {
        job.logs.push(`[STDERR] ${line}`);
      }
    });

    gradleProcess.on('close', (code) => {
      if (code === 0) {
        // Locate generated APK
        const possibleApk = path.join(job.workspaceDir, 'app/build/outputs/apk/debug/app-debug.apk');
        if (fs.existsSync(possibleApk)) {
          job.apkPath = possibleApk;
          job.status = 'SUCCESS';
          job.logs.push('[Server] BUILD SUCCESSFUL! APK generated.');
        } else {
          // Fallback search
          const found = findApkRecursive(job.workspaceDir);
          if (found) {
            job.apkPath = found;
            job.status = 'SUCCESS';
            job.logs.push(`[Server] BUILD SUCCESSFUL! APK generated at ${found}`);
          } else {
            job.status = 'FAILED';
            job.logs.push('[Server] BUILD FAILED: APK artifact not found after build');
          }
        }
      } else {
        job.status = 'FAILED';
        job.logs.push(`[Server] Gradle process exited with error code ${code}`);
      }
    });
  } catch (err) {
    job.status = 'FAILED';
    job.logs.push(`[Server] Execution error: ${err.message}`);
  }
}

function findApkRecursive(dir) {
  const files = fs.readdirSync(dir);
  for (const file of files) {
    const full = path.join(dir, file);
    if (fs.statSync(full).isDirectory()) {
      const res = findApkRecursive(full);
      if (res) return res;
    } else if (file.endsWith('.apk')) {
      return full;
    }
  }
  return null;
}

/**
 * GET /api/v1/build/:jobId/status
 */
app.get('/api/v1/build/:jobId/status', (req, res) => {
  const job = jobs.get(req.params.jobId);
  if (!job) {
    return res.status(404).json({ error: 'Job not found' });
  }
  res.json({
    jobId: job.jobId,
    status: job.status,
    logs: job.logs
  });
});

/**
 * GET /api/v1/build/:jobId/download
 */
app.get('/api/v1/build/:jobId/download', (req, res) => {
  const job = jobs.get(req.params.jobId);
  if (!job || !job.apkPath || !fs.existsSync(job.apkPath)) {
    return res.status(404).json({ error: 'APK artifact not ready or not found' });
  }
  res.download(job.apkPath, `${job.projectName}-debug.apk`);
});

app.listen(PORT, () => {
  console.log(`DroidIDE Cloud Build Server listening on port ${PORT}`);
});
