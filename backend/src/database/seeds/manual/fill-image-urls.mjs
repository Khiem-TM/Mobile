import fs from 'node:fs';
import path from 'node:path';

const ROOT = path.dirname(new URL(import.meta.url).pathname);
const FOODS_CSV = path.join(ROOT, 'foods_vi.csv');
const EXERCISES_CSV = path.join(ROOT, 'exercises_vi.csv');
const MANIFEST_CSV = path.join(ROOT, 'image_upload_manifest.csv');

function parseCsv(text) {
  const rows = [];
  let row = [];
  let cell = '';
  let inQuotes = false;

  for (let i = 0; i < text.length; i += 1) {
    const ch = text[i];
    const next = text[i + 1];

    if (inQuotes) {
      if (ch === '"' && next === '"') {
        cell += '"';
        i += 1;
      } else if (ch === '"') {
        inQuotes = false;
      } else {
        cell += ch;
      }
      continue;
    }

    if (ch === '"') {
      inQuotes = true;
    } else if (ch === ',') {
      row.push(cell);
      cell = '';
    } else if (ch === '\n') {
      row.push(cell.replace(/\r$/, ''));
      rows.push(row);
      row = [];
      cell = '';
    } else {
      cell += ch;
    }
  }

  if (cell.length > 0 || row.length > 0) {
    row.push(cell.replace(/\r$/, ''));
    rows.push(row);
  }

  return rows.filter((r) => r.length > 1 || r[0] !== '');
}

function csvEscape(value) {
  const s = value == null ? '' : String(value);
  if (/[",\n\r]/.test(s)) return `"${s.replace(/"/g, '""')}"`;
  return s;
}

function serializeCsv(rows) {
  return rows.map((row) => row.map(csvEscape).join(',')).join('\n') + '\n';
}

function readTable(filePath) {
  const rows = parseCsv(fs.readFileSync(filePath, 'utf8'));
  const headers = rows[0];
  return {
    headers,
    records: rows.slice(1).map((row) => Object.fromEntries(headers.map((h, i) => [h, row[i] ?? '']))),
  };
}

function writeTable(filePath, headers, records) {
  const rows = [headers, ...records.map((r) => headers.map((h) => r[h] ?? ''))];
  fs.writeFileSync(filePath, serializeCsv(rows), 'utf8');
}

function slugify(input) {
  return input
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .replace(/đ/g, 'd')
    .replace(/Đ/g, 'D')
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/^-+|-+$/g, '')
    .slice(0, 90);
}

function parseArgs(argv) {
  const args = { _: [] };
  for (let i = 0; i < argv.length; i += 1) {
    const cur = argv[i];
    if (!cur.startsWith('--')) {
      args._.push(cur);
      continue;
    }
    const key = cur.slice(2);
    const next = argv[i + 1];
    if (!next || next.startsWith('--')) {
      args[key] = true;
    } else {
      args[key] = next;
      i += 1;
    }
  }
  return args;
}

function buildManifest({ ext = 'jpg', baseUrl = '' } = {}) {
  const foods = readTable(FOODS_CSV).records;
  const exercises = readTable(EXERCISES_CSV).records;
  const normalizedBase = baseUrl.replace(/\/+$/, '');
  const manifest = [
    [
      'table',
      'name',
      'suggested_filename',
      'storage_path',
      'public_url',
      'target_columns',
      'note',
    ],
  ];

  for (const food of foods) {
    const filename = `${slugify(food.name)}.${ext}`;
    const storagePath = `foods/${filename}`;
    const publicUrl = normalizedBase ? `${normalizedBase}/${storagePath}` : '';
    manifest.push([
      'foods',
      food.name,
      filename,
      storagePath,
      publicUrl,
      'image_urls',
      'Upload ảnh món ăn vào bucket public theo storage_path này.',
    ]);
  }

  for (const exercise of exercises) {
    const filename = `${slugify(exercise.name)}.${ext}`;
    const storagePath = `exercises/${filename}`;
    const publicUrl = normalizedBase ? `${normalizedBase}/${storagePath}` : '';
    manifest.push([
      'exercises',
      exercise.name,
      filename,
      storagePath,
      publicUrl,
      'image_avt_url,image_url',
      'Upload ảnh minh họa bài tập vào bucket public theo storage_path này.',
    ]);
  }

  fs.writeFileSync(MANIFEST_CSV, serializeCsv(manifest), 'utf8');
  console.log(`Wrote ${MANIFEST_CSV}`);
}

function applyUrls({ ext = 'jpg', baseUrl }) {
  if (!baseUrl) {
    throw new Error('Missing --base-url. Example: --base-url https://xxx.supabase.co/storage/v1/object/public/vitalai-seed-images');
  }
  const normalizedBase = baseUrl.replace(/\/+$/, '');

  const foods = readTable(FOODS_CSV);
  for (const food of foods.records) {
    const url = `${normalizedBase}/foods/${slugify(food.name)}.${ext}`;
    food.image_urls = `{"${url}"}`;
    food.image_public_ids = '{}';
  }
  writeTable(FOODS_CSV, foods.headers, foods.records);

  const exercises = readTable(EXERCISES_CSV);
  for (const exercise of exercises.records) {
    const url = `${normalizedBase}/exercises/${slugify(exercise.name)}.${ext}`;
    exercise.image_avt_url = url;
    exercise.image_avt_public_id = '';
    exercise.image_url = `{"${url}"}`;
    exercise.image_public_ids = '{}';
  }
  writeTable(EXERCISES_CSV, exercises.headers, exercises.records);

  buildManifest({ ext, baseUrl });
  console.log('Updated image URLs in foods_vi.csv and exercises_vi.csv');
}

const args = parseArgs(process.argv.slice(2));
const command = args._[0] ?? 'manifest';

if (command === 'manifest') {
  buildManifest({ ext: args.ext || 'jpg', baseUrl: args['base-url'] || '' });
} else if (command === 'apply') {
  applyUrls({ ext: args.ext || 'jpg', baseUrl: args['base-url'] });
} else {
  console.error('Usage:');
  console.error('  node fill-image-urls.mjs manifest --ext jpg [--base-url PUBLIC_BUCKET_URL]');
  console.error('  node fill-image-urls.mjs apply --ext jpg --base-url PUBLIC_BUCKET_URL');
  process.exit(1);
}

