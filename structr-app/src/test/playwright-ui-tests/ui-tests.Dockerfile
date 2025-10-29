#
# Copyright (C) 2010-2025 Structr GmbH
#
# This file is part of Structr <http://structr.org>.
#
# Structr is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License as
# published by the Free Software Foundation, either version 3 of the
# License, or (at your option) any later version.
#
# Structr is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with Structr.  If not, see <http://www.gnu.org/licenses/>.
#

FROM mcr.microsoft.com/playwright:v1.56.0-jammy

# Install Node.js (if not already in the base image)
RUN apt-get update && apt-get install -y curl && \
    curl -fsSL https://deb.nodesource.com/setup_22.x | bash - && \
    apt-get install -y nodejs && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# Set working directory
WORKDIR /tests

# Copy test files and configuration
ADD ./ ./
# Install dependencies
RUN npm install && npx playwright install chromium

# Set environment variables
ENV BASE_URL=http://structr-ui-tests:8082
ENV SUPERUSER_PASSWORD=structr1234
ENV CI=true

# Run tests
CMD ["npm", "run", "test"]