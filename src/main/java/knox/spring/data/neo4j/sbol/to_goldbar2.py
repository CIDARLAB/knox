# from start_with_dict import Exp, Or, Then, ZeroOrMore, OneOrMore, ZeroOrOne


def then_to_goldbar(then):
	if then.exps[0].exp_type != "Exp":
		s = "(" + to_goldbar(then.exps[0]) + ")"
	else:
		s = to_goldbar(then.exps[0])

	for x in then.exps[1:]:
		if x.exp_type != "Exp":
			s += " then (" + to_goldbar(x) + ")"
		else:
			s += " then " + to_goldbar(x)
	return s


def or_to_goldbar(or_exp):
	if or_exp.exps[0].exp_type != "Exp":
		s = "(" + to_goldbar(or_exp.exps[0]) + ")"
	else:
		s = to_goldbar(or_exp.exps[0])
		
	for x in or_exp.exps[1:]:
		if x.exp_type != "Exp":
			s += " or (" + to_goldbar(x) + ")"
		else:
			s += " or " + to_goldbar(x)
	return s



def to_goldbar(root):
	if root.exp_type == "Exp":
		if root.name == "e":
			return ""
		else:
			return root.name

	elif root.exp_type == "OneOrMore":
		return "one-or-more("+to_goldbar(root.exp)+")"

	elif root.exp_type == "ZeroOrMore":
		return "zero-or-more("+to_goldbar(root.exp)+")"

	elif root.exp_type == "ZeroOrOne":
		return "zero-or-one("+to_goldbar(root.exp)+")"

	elif root.exp_type == "Then":
		return then_to_goldbar(root)

	elif root.exp_type == "Or":
		return or_to_goldbar(root)

	else:
		return root






# print(to_goldbar(Or([OneOrMore(Exp("promoter")), Then([OneOrMore(Exp("promoter")), OneOrMore(Exp("cds"))])])))
# print(to_goldbar(Then([Exp("promoter"), Exp("cds")])))
# print(to_goldbar(Or([Then([Exp("promoter"), OneOrMore(Exp("cds"))]), Then([Exp("promoter"), OneOrMore(Exp("cds")), OneOrMore(Exp("rbs"))])])))
# print(to_goldbar(Then([Exp("cds"), Or([Exp("ribosome_entry_site"), Exp("promoter")])])))





# Or([
# 	Then([
# 		Exp("promoter"), 
# 		Exp("cds"), 
# 		ZeroOrMore(Exp("cds"))
# 	]), 
# 	Then([
# 		Exp("promoter"), 
# 		Exp("cds"), 
# 		ZeroOrMore(Exp("cds")), 
# 		Exp("rbs"), 
# 		ZeroOrMore(Exp("rbs"))
# 	])
# ])































