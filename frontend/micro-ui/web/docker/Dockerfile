FROM upyogio/alpine-node-builder-14:yarn AS build
#FROM ghcr.io/egovernments/alpine-node-builder-14:yarn AS build
RUN apk update && apk upgrade
RUN apk add --no-cache git>2.30.0
RUN yarn add @nudmcdgnpm/digit-ui-libraries
RUN yarn add @upyog/digit-ui-module-fsm
RUN yarn add @upyog/digit-ui-module-pt
RUN yarn add @upyog/digit-ui-module-tl
RUN yarn add @upyog/digit-ui-module-obps
RUN yarn add @upyog/digit-ui-module-ws
RUN yarn add @upyog/digit-ui-module-dss
RUN yarn add @upyog/digit-ui-module-mcollect
RUN yarn add @nudmcdgnpm/digit-ui-module-pgr
RUN yarn add @upyog/digit-ui-module-noc
RUN yarn add @upyog/digit-ui-module-engagement
RUN yarn add @mcd89/digit-ui-module-hrms
RUN yarn add @upyog/digit-ui-module-core
RUN yarn add @upyog/digit-ui-module-common
RUN yarn add @upyog/digit-ui-module-commonpt
RUN yarn add @upyog/digit-ui-module-bills
RUN yarn add @nudmcdgnpm/upyog-ui-module-ptr
RUN yarn add @nudmcdgnpm/upyog-ui-module-cm
RUN yarn add @nudmcdgnpm/upyog-ui-module-ads
RUN yarn add @nudmcdgnpm/upyog-ui-module-chb
RUN yarn add @nudmcdgnpm/upyog-ui-module-sv
RUN yarn add @nudmcdgnpm/upyog-ui-module-asset
RUN yarn add @nudmcdgnpm/upyog-ui-module-ew
RUN yarn add @nudmcdgnpm/upyog-ui-module-vendor
RUN yarn add @nudmcdgnpm/upyog-ui-module-wt

ARG WORK_DIR
WORKDIR /app
ENV NODE_OPTIONS "--max-old-space-size=8192"
ENV GENERATE_SOURCEMAP "false"

COPY ${WORK_DIR} .
RUN ls -lah

#RUN node web/envs.js
RUN cd web/ \
    && node envs.js \
    && node -e 'console.log(v8.getHeapStatistics().heap_size_limit/(8192*8192))' \
    &&  ./install-deps.sh \
    && yarn install \
    && yarn build 
    

FROM nginx:mainline-alpine
#FROM ghcr.io/egovernments/nginx:mainline-alpine
ENV WORK_DIR=/var/web/digit-ui

RUN mkdir -p ${WORK_DIR}

COPY --from=build /app/web/build ${WORK_DIR}/
COPY --from=build /app/web/docker/nginx.conf /etc/nginx/conf.d/default.conf
