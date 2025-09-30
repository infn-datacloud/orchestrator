FROM mcr.microsoft.com/devcontainers/typescript-node:18

# Install orchent
ARG TARGETPLATFORM
ARG TARGETOS
ARG TARGETARCH

ARG ORCHENT_VERSION=1.3.0

RUN if [ "${TARGETOS}" = "linux" ]; then \
    wget https://repo.cloud.cnaf.infn.it/repository/orchent/orchent-${TARGETARCH}/${ORCHENT_VERSION}/orchent-${TARGETARCH}-${ORCHENT_VERSION}.deb && \
    dpkg -i orchent-${TARGETARCH}-${ORCHENT_VERSION}.deb && \
    rm -rf orchent-${TARGETARCH}-${ORCHENT_VERSION}.deb; \
    fi

