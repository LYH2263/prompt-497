# Build stage
FROM node:20-alpine AS build
# Set registry to Taobao mirror for faster install
RUN npm config set registry https://registry.npmmirror.com

WORKDIR /app
COPY package*.json ./
RUN npm install
COPY . .
RUN npm run build

# Production stage
FROM nginx:alpine
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
