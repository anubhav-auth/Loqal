#!/bin/bash

ENV_FILE=".env"
EXAMPLE_FILE=".env.example"

echo "📦 Starting .env generation script..."

# Check if .env already exists
if [ -f "$ENV_FILE" ]; then
  echo "⚠️  $ENV_FILE already exists. No action taken."
  exit 0
fi

# Check if .env.example exists
if [ ! -f "$EXAMPLE_FILE" ]; then
  echo "❌ $EXAMPLE_FILE not found. Please ensure it exists in the project root."
  exit 1
fi

# Copy .env.example to .env
cp "$EXAMPLE_FILE" "$ENV_FILE"
echo "✅ Created $ENV_FILE from $EXAMPLE_FILE."

# Optional: Prompt to edit now
read -p "✏️  Do you want to open $ENV_FILE now to fill in your real secrets? (y/n): " EDIT_NOW

if [[ "$EDIT_NOW" =~ ^[Yy]$ ]]; then
  ${EDITOR:-nano} "$ENV_FILE"
fi

echo "🎉 Done. You can now run your notification microservice!"
