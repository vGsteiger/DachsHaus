// Apollo server plugin for tracing
export const tracingPlugin = {
  async requestDidStart() {
    return {
      async willSendResponse() {
        // Add tracing logic
      },
    };
  },
};
