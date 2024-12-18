package jadescript.core.behaviours;


import jadescript.java.AgentEnv;
import jadescript.java.SideEffectsFlag;

public class OneShotBehaviour<A extends jadescript.core.Agent>
        extends Behaviour<A> implements OneShot {
    @Override
    protected Behaviour.ExecutionType __executionType() {
        return ExecutionType.OneShot;
    }

    public OneShotBehaviour(
        AgentEnv<? extends A, ? extends SideEffectsFlag.WithSideEffects> _agentEnv
    ) {
        super(_agentEnv);
    }

    @Override
    public void doAction(int _tickCount) {

    }

    @Override
    public void doOnActivate() {

    }

    @Override
    public void doOnDeactivate() {

    }

    @Override
    public void doOnDestroy() {

    }



    @SuppressWarnings("rawtypes")
    public static OneShotBehaviour __createEmpty(){
        return new EmptyOneShotBehaviour();
    }

    public static <T extends jadescript.core.Agent> OneShotBehaviour<T>
    __createEmptyWithEnv(
        AgentEnv<? extends T, ? extends SideEffectsFlag.WithSideEffects>
            _agentEnv
    ){
        return new OneShotBehaviour<>(_agentEnv);
    }

    @SuppressWarnings({ "rawtypes", "serial" })
    public static class EmptyOneShotBehaviour extends OneShotBehaviour {
        public EmptyOneShotBehaviour() {
			super(null);
		}

		@Override
        public void doAction(int _tickCount) {
            throw new UninitializedBehaviourException();
        }

        @Override
        public void doOnActivate() {
            throw new UninitializedBehaviourException();
        }
    }

}
