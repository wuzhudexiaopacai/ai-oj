# 多阶段构建：rust 编译 -> debian slim 运行
# 需 1.85+：部分最新版依赖（bollard/thiserror 等）已声明 edition 2024
FROM rust:1.90-slim-bookworm AS builder
WORKDIR /app
# Cargo 国内镜像配置（rsproxy），必须在拉依赖前生效
COPY cargo-config.toml /usr/local/cargo/config.toml
COPY Cargo.toml ./
COPY src ./src
RUN cargo build --release

FROM debian:bookworm-slim
RUN apt-get update \
    && apt-get install -y --no-install-recommends ca-certificates \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY --from=builder /app/target/release/ai-oj-sandbox /app/ai-oj-sandbox
COPY config.toml languages.toml /app/

# 工作目录挂载点：由 docker run -v ai-oj-work:/work 挂载共享 volume
RUN mkdir -p /work
VOLUME ["/work"]

EXPOSE 8000
CMD ["/app/ai-oj-sandbox"]
