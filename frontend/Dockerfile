FROM node:20-alpine AS build
WORKDIR /app
# 换 npm 国内源（淘宝镜像），否则 docker build 里 npm install 会非常慢
RUN npm config set registry https://registry.npmmirror.com
COPY package.json package-lock.json ./
RUN npm install
COPY . .
RUN npm run build

FROM nginx:alpine
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80