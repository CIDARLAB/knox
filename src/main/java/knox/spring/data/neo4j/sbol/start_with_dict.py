import copy
import simp2
import to_goldbar2
import argparse
import ast

class Exp:
	def __init__(self, name):
		self.exp_type = "Exp"
		self.name = name

	def __str__(self):
		return "Exp("+str(self.name)+")"

	def __repr__(self):
		return str(self)

	def __eq__(self, other):
		return hash(str(self)) == hash(str(other))


class Or(Exp):
	def __init__(self, exps):
		self.exp_type = "Or"
		self.name = "or"
		self.exps = exps

	def __str__(self):
		return "Or("+str(self.exps)+")"

	def __repr__(self):
		return str(self)

class Then(Exp):
	def __init__(self, exps):
		self.exp_type = "Then"
		self.name = "then"
		self.exps = exps

	def __str__(self):
		return "Then("+str(self.exps)+")"

	def __repr__(self):
		return str(self)

class ZeroOrMore(Exp):
	def __init__(self, exp):
		self.exp_type = "ZeroOrMore"
		self.name = "zero-or-more"
		self.exp = exp

	def __str__(self):
		return "ZeroOrMore("+str(self.exp)+")"
	def __repr__(self):
		return str(self)

class OneOrMore(Exp):
	def __init__(self, exp):
		self.exp_type = "OneOrMore"
		self.name = "one-or-more"
		self.exp = exp

	def __str__(self):
		return "OneOrMore("+str(self.exp)+")"

	def __repr__(self):
		return str(self)

class ZeroOrOne(Exp):
	def __init__(self, exp):
		self.exp_type = "ZeroOrOne"
		self.name = "zero-or-one"
		self.exp = exp

	def __str__(self):
		return "ZeroOrOne("+str(self.exp)+")"

	def __repr__(self):
		return str(self)


class DFA:
	def __init__(self, states, init_state, final_states, transition_funct):
		self.states = states
		self.init_state = init_state
		self.final_states = final_states
		self.transition_funct = transition_funct
		self.regex = ''
		self.ds = {}
		self.transition_dict = {}
		self.set_transition_dict()


	def format_nested(self, exp_type, parts):
		if len(parts) == 0:
			return Exp("")
		elif len(parts) == 1:
			return parts[0]
		else:
			return exp_type(parts[0], self.format_nested(exp_type, parts[1:]))


	def set_transition_dict(self):
		dict_states = {r: {c: Exp('_') for c in self.states} for r in self.states}
		for key in self.transition_funct:
			val = self.transition_funct[key]
			for v_key in val:
				if val[v_key] != "_":
					parts = val[v_key].split(", ")
					if len(parts)==1:
						dict_states[key][v_key] = Exp(val[v_key])
					else:
						for i in range(len(parts)):
							parts[i] = Exp(parts[i])
						dict_states[key][v_key] = Or(parts)


		self.ds = dict_states
		self.transition_dict = copy.deepcopy(dict_states)

	def get_intermediate_states(self):
		return [state for state in self.states if state not in ([self.init_state] + self.final_states)]

	def get_predecessors(self, state):
		return [key for key, value in self.ds.items() if state in value.keys() and value[state].name != '_' and key != state]

	def get_successors(self, state):
		return [key for key, value in self.ds[state].items() if value.name != '_' and key != state]

	def get_if_loop(self, state):
		if self.ds[state][state].name != '_':
			return self.ds[state][state]
		else:
			return Exp("_")

	# creates a ZeroOrMore object
	def format_zero_or_more(self, loop):
		if loop.name != "_" and len(loop.name)>0:
			return ZeroOrMore(loop)
		else:
			return loop

	# creates a OneOrMore object
	def format_one_or_more(self, loop):
		if loop.name != "_" and len(loop.name)>0:
			return OneOrMore(loop)
		else:
			return loop

	# checks if a pair of expressions can form a one-or-more
	def check_plus(self, pred_to_inter, inter_loop):
		if pred_to_inter.name == inter_loop.name:
			return True
		else:
			return False

	# formats path to remove epsilons
	def format_new_path(self, path_parts):
		non_blanks = []
		for x in path_parts:
			if len(x.name) != 0 and x.name != "e" and x.name != "_":
			# if len(x) != 0 and x != "":
				non_blanks += [x]

		print("non_blanks --> ", non_blanks)

		if len(non_blanks) == 0:
			return Exp("e")
		elif len(non_blanks) == 1:
			return non_blanks[0]
		else:
			then_list = []
			for part in non_blanks:
				if part.exp_type == "Then":
					then_list += part.exps
				else:
					then_list += [part]

			new_path = Then(then_list)
			return new_path


	# formats the expression that goes into the state dictionary
	def format_entry(self, entry, i, j, exp):
		if entry.name == "_" or len(entry.name)==0:
			return exp
		else:
			if i == j:
				return Or([ZeroOrMore(entry), exp])
			else:
				return Or([entry, exp])


	def toregex(self):
		intermediate_states = self.get_intermediate_states()
		dict_states = self.ds

		for inter in intermediate_states:
			predecessors = self.get_predecessors(inter)
			successors = self.get_successors(inter)
			print('inter : ', inter)
			print('predecessor : ', predecessors)
			print('successor : ', successors)

			print("dict before inner loop --> ", dict_states)

			for i in predecessors:
				# to_add = {}
				for j in successors:
					if dict_states[inter]['FINAL'] == Exp('e') and i == j:
						continue
					inter_loop = self.get_if_loop(inter)
					pred_loop = self.format_zero_or_more(self.get_if_loop(i))
					print('i and j : ', i, j)
					print("inter_loop", inter_loop)
					print("pred_loop", pred_loop)

					pred_to_inter = dict_states[i][inter]
					inter_to_succ = dict_states[inter][j]
					print("pred_to_inter", pred_to_inter)
					print("inter_to_succ", inter_to_succ)

					if self.check_plus(pred_to_inter, inter_loop):
						inter_loop = self.format_one_or_more(inter_loop)
						new_path = self.format_new_path([pred_loop, inter_loop, inter_to_succ])
					else:
						inter_loop = self.format_zero_or_more(inter_loop)
						pred_to_inter = pred_to_inter
						new_path = self.format_new_path([pred_loop, pred_to_inter,  inter_loop, inter_to_succ])

					print("new_path --> ", new_path, "\n")
					# to_add[j] = self.format_entry(dict_states[i][j], i, j, new_path)
					dict_states[i][j] = self.format_entry(dict_states[i][j], i, j, new_path)
				# for key in to_add:
				# 	val = to_add[key]
				# 	dict_states[i][key] = val
				# dict_states[i][j] = self.format_entry(dict_states[i][j], i, j, new_path)
			dict_states = {r: {c: v for c, v in val.items() if c != inter} for r, val in dict_states.items() if
					   r != inter}  # remove inter node
			self.ds = copy.deepcopy(dict_states)

		return dict_states[self.init_state][self.final_states[0]]



def main():
	# # ZERO OR MORE
	# states = "n0 n1 n2 n3"
	# states = states.split()
	# init_state = "n1"
	# final_states = "n3"
	# final_states = final_states.split()
	# transition_matrix = [["_", "_", "e", "e"], ["_", "_", "e", "e"], ["promoter", "_", "_", "_"], ["_", "_", "_", "_"]]
	# transition_funct = dict(zip(states, transition_matrix))

	# # ONE OR MORE
	# states = "n0 n1 n2"
	# states = states.split()
	# init_state = "n2"
	# final_states = "n1"
	# final_states = final_states.split()
	# transition_matrix = [["_", "promoter", "_"], ["e", "_", "_"], ["e", "_", "_"]]
	# transition_funct = dict(zip(states, transition_matrix))

	# # OR
	# states = "n0 n1"
	# states = states.split()
	# init_state = "n1"
	# final_states = "n0"
	# final_states = final_states.split()
	# transition_matrix = [["_", "_"], ["cds, promoter", "_"]]
	# transition_funct = dict(zip(states, transition_matrix))


	# # THEN
	# states = "n0 n1 n3"
	# states = states.split()
	# init_state = "n0"
	# final_states = "n3"
	# final_states = final_states.split()
	# transition_matrix = [["_", "promoter", "_"], ["_", "_", "cds"], ["_", "_", "_"]]
	# transition_funct = dict(zip(states, transition_matrix))


	# # COMPLEX
	# states = "n0 n1 n2 n3 n5 n6"
	# states = states.split()
	# init_state = "n1"
	# final_states = "n5"
	# final_states = final_states.split()
	# transition_matrix = [["_", "_", "promoter", "_", "_", "_"], ["e", "_", "_", "_", "_", "_"], ["e", "_", "_", "e", "e", "_"], ["_", "_", "_", "_", "_", "cds"], ["_", "_", "_", "_", "_", "_"], ["_", "_", "_", "e", "e", "_"]]
	# # transition_matrix = [["_", "e", "_", "_", "_", "_"], ["_", "_", "promoter", "_", "_", "_"], ["_", "e", "_", "e", "_", "e"], ["_", "_", "_", "_", "cds", "_"], ["_", "_", "_", "e", "_", "e"], ["_", "_", "_", "_", "_", "_"]]
	# transition_funct = dict(zip(states, transition_matrix))


	# # COMPLEX1
	# # funct --> {n0={n0=_, n1=_, n2=none, n3=_, n5=_, n6=_, n7=_}, n1={n0=promoter, n1=_, n2=_, n3=_, n5=_, n6=_, n7=_}, n2={n0=_, n1=_, n2=_, n3=cds, n5=_, n6=_, n7=_}, n3={n0=_, n1=_, n2=none, n3=_, n5=none, n6=_, n7=none}, n5={n0=_, n1=_, n2=_, n3=_, n5=_, n6=ribosome_entry_site, n7=_}, n6={n0=_, n1=_, n2=_, n3=_, n5=none, n6=_, n7=none}, n7={n0=_, n1=_, n2=_, n3=_, n5=_, n6=_, n7=_}}
	# states = "n0 n1 n2 n3 n5 n6 n7"
	# states = states.split()
	# init_state = "n1"
	# final_states = "n7"
	# final_states = final_states.split()
	# transition_matrix = [["_", "_", "e", "_", "_", "_", "_"], ["promoter", "_", "_", "_", "_", "_", "_"], ["_", "_", "_", "cds", "_", "_", "_"], ["_", "_", "e", "_", "e", "_", "e"], ["_", "_", "_", "_", "_", "rbs", "_"], ["_", "_", "_", "_", "e", "_", "e"], ["_", "_", "_", "_", "_", "_", "_"]]
	# # transition_matrix = [["_", "e", "_", "_", "_", "_"], ["_", "_", "promoter", "_", "_", "_"], ["_", "e", "_", "e", "_", "e"], ["_", "_", "_", "_", "cds", "_"], ["_", "_", "_", "e", "_", "e"], ["_", "_", "_", "_", "_", "_"]]
	# transition_funct = dict(zip(states, transition_matrix))

	# COMPLEX3
	# funct --> {n0={n0=_, n1=cds, n2=_, n4=_, n5=_}, n1={n0=_, n1=_, n2=none, n4=_, n5=none}, n2={n0=_, n1=_, n2=_, n4=ribosome_entry_site, n5=_}, n4={n0=_, n1=_, n2=none, n4=_, n5=none}, n5={n0=_, n1=_, n2=_, n4=_, n5=_}}
	# states = "n0 n1 n2 n4 n5"
	# states = states.split()
	# init_state = "n0"
	# final_states = "n5"
	# final_states = final_states.split()
	# transition_matrix = [["_", "cds", "_", "_", "_"], ["_", "_", "e", "_", "promoter, e"], ["_", "_", "_", "ribosome_entry_site", "_"], ["_", "_", "e", "_", "e"], ["_", "_", "_", "_", "_"]]
	# transition_matrix = [["_", "e", "_", "_", "_", "_"], ["_", "_", "promoter", "_", "_", "_"], ["_", "e", "_", "e", "_", "e"], ["_", "_", "_", "_", "cds", "_"], ["_", "_", "_", "e", "_", "e"], ["_", "_", "_", "_", "_", "_"]]
	# transition_funct = dict(zip(states, transition_matrix))


	parser = argparse.ArgumentParser(fromfile_prefix_chars='@')
	parser.add_argument('-states', type=str, dest="states")
	parser.add_argument('-start', type=str, dest="start")
	parser.add_argument('-accept', type=str, dest="final")
	parser.add_argument('-transition', type=str, dest="transition_funct")
	args = parser.parse_args(['@/Users/vidyaakavoor/Documents/Knox_base/knox/src/main/java/knox/spring/data/neo4j/sbol/args.txt'])

	# print(sys.argv)
	states = args.states.split()
	print(states)
	init_state = args.start
	print(init_state)
	final_states = [args.final]
	print(final_states)
	transition_funct = args.transition_funct
	# print(transition_funct)
	transition_funct = ast.literal_eval(transition_funct)
	print(transition_funct)

	

	# print("transition_funct --> ", transition_funct)

	# transition_funct = {"n0":{"n0":"_", "n1":"cds", "n2":"_", "n4":"_", "n5":"_"}, "n1":{"n0":"_", "n1":"_", "n2":"e", "n4":"_", "n5":"promoter, e"}, "n2":{"n0":"_", "n1":"_", "n2":"_", "n4":"ribosome_entry_site", "n5":"_"}, "n4":{"n0":"_", "n1":"_", "n2":"e", "n4":"_", "n5":"e"}, "n5":{"n0":"_", "n1":"_", "n2":"_", "n4":"_", "n5":"_"}}

	# create a start node with an empty edge to the actual first edge ('e' is the epsilon)
	start_array = {}
	for key in transition_funct:
		val = transition_funct[key]
		if key == init_state:
			start_array[key] = 'e'
		else:
			start_array[key] = '_'
		val['START'] = '_'
		transition_funct[key] = val
	start_array['FINAL'] = '_'
	transition_funct['START'] = start_array

	# create a final node with an empty edge pointing to it from the actual final node ('e' is the epsilon)
	final_array = {}
	for key in transition_funct:
		val = transition_funct[key]
		if key == final_states[0]:
			val['FINAL'] = 'e'
		else:
			val['FINAL'] = '_'
		final_array[key] = '_'
		transition_funct[key] = val
	final_array['FINAL'] = '_'
	transition_funct['FINAL'] = final_array

	print("transition_funct --> ", transition_funct)

	# redefine the initial and final states and the state list
	init_state = 'START'
	final_states = ['FINAL']
	states += ['START', 'FINAL']

	r = Exp("")
	simp = Exp("")
	goldbar = ""

	for f in final_states:
		dfa = DFA(states, init_state, [f], transition_funct)
		print("dict --> ", dfa.transition_dict)
		r = dfa.toregex()
		simp = simp2.simplify_regex(r)
		goldbar = to_goldbar2.to_goldbar(simp)

	print(goldbar)
	return goldbar


if __name__ == '__main__':
	main()


