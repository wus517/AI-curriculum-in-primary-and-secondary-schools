import { readFileSync, writeFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';

const root = new URL('./', import.meta.url);
const schemas = JSON.parse(readFileSync(new URL('openapi.yaml', root), 'utf8')).components.schemas;

function type(schema) {
  if (schema.$ref) return schema.$ref.split('/').pop();
  if (schema.const !== undefined) return JSON.stringify(schema.const);
  if (schema.enum) return schema.enum.map(value => JSON.stringify(value)).join(' | ');
  if (schema.anyOf) return schema.anyOf.map(type).join(' | ');
  if (schema.oneOf && !schema.properties) return schema.oneOf.map(type).join(' | ');
  if (schema.type === 'null') return 'null';
  if (schema.type === 'integer' || schema.type === 'number') return 'number';
  if (schema.type === 'array') return `Array<${type(schema.items)}>`;
  if (schema.type === 'object') {
    if (!schema.properties) return schema.additionalProperties
      ? `Record<string, ${type(schema.additionalProperties)}>` : 'Record<string, never>';
    return '{\n' + Object.entries(schema.properties).map(([key, value]) =>
      `  ${key}${schema.required?.includes(key) ? '' : '?'}: ${type(value)};`).join('\n') + '\n}';
  }
  return schema.type ?? 'unknown';
}

const content = '// Generated from openapi.yaml; new contract IDs are decimal strings.\n'
  + Object.entries(schemas).filter(([name]) => !name.endsWith('Response'))
    .map(([name, schema]) => `export type ${name} = ${type(schema)};`).join('\n\n') + '\n';
const target = new URL('types.ts', root);
if (process.argv.includes('--check')) {
  if (readFileSync(target, 'utf8') !== content) {
    throw new Error(`Contract types out of date: ${fileURLToPath(target)}`);
  }
  process.stdout.write('Contract types match OpenAPI.\n');
} else {
  writeFileSync(target, content);
}
